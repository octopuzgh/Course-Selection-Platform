package com.octopuz.spark

import com.alibaba.fastjson.JSON
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions.{col, from_json}
import org.apache.spark.sql.types.{StringType, StructField, StructType}
import redis.clients.jedis.Jedis
import org.yaml.snakeyaml.Yaml

import java.io.FileInputStream
import java.util

object StreamingStatsApplication {

  case class SelectionMessage(studentNo: String, courseNo: String, `type`: String)

  val RANKING_KEY = "course:ranking"
  val STOCK_KEY_PREFIX = "course:stock:"
  val STATS_TOTAL_KEY = "stats:total"
  val STATS_TODAY_COUNT_KEY = "stats:today:count"
  val STATS_TODAY_STUDENTS_KEY = "stats:today:students"

  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder
      .appName("RealtimeSelectionStats")
      .getOrCreate()

    import spark.implicits._

    val yaml = new Yaml()
    val config = yaml.load(new FileInputStream("src/main/resources/application.yml"))
      .asInstanceOf[util.LinkedHashMap[String, util.LinkedHashMap[String, Any]]]

    val kafkaConfig = config.get("kafka")
    val kafkaBrokers = kafkaConfig.get("bootstrap-servers").asInstanceOf[String]
    val kafkaTopic = kafkaConfig.get("topic").asInstanceOf[String]
    val kafkaGroupId = kafkaConfig.get("group-id").asInstanceOf[String]

    val redisConfig = config.get("redis")
    val redisHost = redisConfig.get("host").asInstanceOf[String]
    val redisPort = redisConfig.get("port").asInstanceOf[Int]
    val redisDb = redisConfig.get("database").asInstanceOf[Int]

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

    val query = parsed.writeStream
      .foreachBatch { (batchDF, epochId) =>
        val jedis = new Jedis(redisHost, redisPort, 3000)
        jedis.select(redisDb)

        val messages = batchDF.as[SelectionMessage].collect()
        messages.foreach { msg =>
          if (msg.`type` == "SELECT") {
            jedis.decr(STOCK_KEY_PREFIX + msg.courseNo)
            jedis.zincrby(RANKING_KEY, -1, msg.courseNo)
            jedis.incr(STATS_TOTAL_KEY)
            jedis.incr(STATS_TODAY_COUNT_KEY)
            jedis.sadd(STATS_TODAY_STUDENTS_KEY, msg.studentNo)
          } else if (msg.`type` == "DROP") {
            jedis.incr(STOCK_KEY_PREFIX + msg.courseNo)
            jedis.zincrby(RANKING_KEY, 1, msg.courseNo)
            jedis.decr(STATS_TOTAL_KEY)
            jedis.decr(STATS_TODAY_COUNT_KEY)
            jedis.srem(STATS_TODAY_STUDENTS_KEY, msg.studentNo)
          }
        }

        jedis.close()
        println(s"[Batch $epochId] Processed ${messages.length} messages")
      }
      .option("checkpointLocation", "/tmp/spark/checkpoint/realtime_stats")
      .outputMode("append")
      .start()

    query.awaitTermination()
  }
}