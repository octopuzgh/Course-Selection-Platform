package com.octopuz.spark

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.streaming.Trigger
import org.apache.spark.sql.types._
import org.apache.spark.sql.functions._
import org.yaml.snakeyaml.Yaml
import redis.clients.jedis.Jedis

import java.time.LocalDate
import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

object StreamingStatsApplication {

  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder()
      .appName("RealtimeSelectionStats")
      .master("local[*]")
      .config("spark.sql.shuffle.partitions", "2")
      .getOrCreate()

    import spark.implicits._

    // 从 classpath 读取配置
    val yaml = new Yaml()
    val is = this.getClass.getResourceAsStream("/application.yml")
    if (is == null) {
      println("Error: application.yml not found in classpath")
      sys.exit(1)
    }

    val config = try {
      yaml.load(is).asInstanceOf[java.util.LinkedHashMap[String, java.util.LinkedHashMap[String, Any]]]
    } catch {
      case e: Exception =>
        println(s"Error loading configuration: ${e.getMessage}")
        sys.exit(1)
    } finally {
      try is.close() catch { case _: Exception => }
    }

    // 安全获取配置项
    val kafkaConfig = config.getOrDefault("kafka", new java.util.LinkedHashMap[String, Any]())
    val redisConfig = config.getOrDefault("redis", new java.util.LinkedHashMap[String, Any]())

    val kafkaBrokers = kafkaConfig.getOrDefault("bootstrap-servers", "localhost:9092").asInstanceOf[String]
    val kafkaTopic = kafkaConfig.getOrDefault("topic", "selection-events").asInstanceOf[String]
    val kafkaGroupId = kafkaConfig.getOrDefault("group-id", "spark-streaming-group").asInstanceOf[String]
    val redisHost = redisConfig.getOrDefault("host", "localhost").asInstanceOf[String]
    val redisPort = redisConfig.getOrDefault("port", 6379) match {
      case p: Int => p
      case p: String => p.toInt
      case _ => 6379
    }

    val redisDb = redisConfig.getOrDefault("database", 0) match {
      case d: Int => d
      case d: String => d.toInt
      case _ => 0
    }

    val redisUsername = redisConfig.getOrDefault("username", "default") match {
      case u: String => u
      case _ => "default"
    }

    val redisPassword = redisConfig.getOrDefault("password", "") match {
      case p: String => p
      case _ => ""
    }

    // 从环境变量或默认值获取 checkpoint 路径
    val checkpointPath = System.getenv().getOrDefault("SPARK_CHECKPOINT_PATH", "/tmp/spark/checkpoint/realtime_stats")

    val schema = StructType(Seq(
      StructField("studentNo", StringType),
      StructField("courseNo", StringType),
      StructField("type", StringType)
    ))

    val df = spark.readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", kafkaBrokers)
      .option("subscribe", kafkaTopic)
      .option("group.id", kafkaGroupId)
      .option("startingOffsets", "latest")
      .option("failOnDataLoss", "false")
      .load()

    val parsed = df.selectExpr("CAST(value AS STRING) as json_str")
      .select(from_json(col("json_str"), schema).alias("data"))
      .select("data.*")
      .filter(col("studentNo").isNotNull && col("courseNo").isNotNull && col("type").isNotNull)

    val query = parsed.writeStream
      .foreachBatch { (batchDF: org.apache.spark.sql.Dataset[org.apache.spark.sql.Row], epochId: Long) =>
        batchDF.foreachPartition { (partition: Iterator[org.apache.spark.sql.Row]) =>
          var jedis: Jedis = null
          try {
            jedis = new Jedis(redisHost, redisPort)
            if (redisPassword.nonEmpty) {
              jedis.auth(redisUsername, redisPassword)
            }
            jedis.select(redisDb)
            val today = LocalDate.now().toString

            // 使用 pipeline 提高性能
            val pipeline = jedis.pipelined()

            partition.foreach { (row: org.apache.spark.sql.Row) =>
              try {
                val studentNo = row.getAs[String]("studentNo")
                val courseNo = row.getAs[String]("courseNo")
                val msgType = row.getAs[String]("type")

                if (msgType != null) {
                  if (msgType == "SELECT") {
                    // 每日统计
                    pipeline.incr(s"stats:daily:count:$today")
                    pipeline.sadd(s"stats:daily:students:$today", studentNo)
                    pipeline.expire(s"stats:daily:count:$today", 172800)
                    pipeline.expire(s"stats:daily:students:$today", 172800)

                    // 今日排行榜
                    pipeline.zincrby(s"course:popularity:$today", 1, courseNo)
                    pipeline.expire(s"course:popularity:$today", 172800)

                    // 累计统计
                    pipeline.incr("stats:total")
                    pipeline.incr("stats:today:count")
                    pipeline.sadd("stats:today:students", studentNo)

                  } else if (msgType == "DROP") {
                    // 每日统计（减少计数并移除学生）
                    pipeline.decr(s"stats:daily:count:$today")
                    pipeline.srem(s"stats:daily:students:$today", studentNo)
                    pipeline.zincrby(s"course:popularity:$today", -1, courseNo)

                    // 累计统计（减少计数并移除学生）
                    pipeline.decr("stats:total")
                    pipeline.decr("stats:today:count")
                    pipeline.srem("stats:today:students", studentNo)
                  }
                }
              } catch {
                case e: Exception =>
                  println(s"Error processing record: ${e.getMessage}")
                  e.printStackTrace()
              }
            }

            // 执行所有管道命令
            pipeline.sync()

          } catch {
            case e: Exception =>
              println(s"Redis connection error: ${e.getMessage}")
              e.printStackTrace()
          } finally {
            if (jedis != null) {
              try {
                jedis.close()
              } catch {
                case e: Exception =>
                  println(s"Error closing Redis connection: ${e.getMessage}")
              }
            }
          }
        }
        println(s"[Batch $epochId] processed at ${java.time.LocalDateTime.now()}")
      }
      .option("checkpointLocation", checkpointPath)
      .outputMode("append")
      .start()

    // 添加关闭钩子
    sys.addShutdownHook({
      println("Shutting down Spark Streaming query...")
      query.stop()
    })

    query.awaitTermination()
  }
}
