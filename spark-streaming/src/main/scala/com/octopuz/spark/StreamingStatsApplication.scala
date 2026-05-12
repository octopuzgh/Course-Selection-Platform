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

  val DAILY_COUNT_KEY = "stats:daily:count"
  val DAILY_STUDENTS_KEY = "stats:daily:students"
  val COURSE_POPULARITY_KEY_PREFIX = "course:popularity:"

  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder
      .appName("RealtimeSelectionStats")
      .getOrCreate()

    import spark.implicits._

    val yaml = new Yaml()
    val config = yaml.load(new FileInputStream("src/main/resources/application.yml"))
      .asInstanceOf[LinkedHashMap[String, LinkedHashMap[String, Any]]]

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

        val today = LocalDate.now().toString
        val dailyCountKey = DAILY_COUNT_KEY + ":" + today
        val dailyStudentsKey = DAILY_STUDENTS_KEY + ":" + today

        val messages = batchDF.as[SelectionMessage].collect()
        messages.foreach { msg =>
          if (msg.`type` == "SELECT") {
            jedis.incr(dailyCountKey)
            jedis.sadd(dailyStudentsKey, msg.studentNo)
            jedis.expire(dailyCountKey, 172800)
            jedis.expire(dailyStudentsKey, 172800)

            jedis.zincrby(COURSE_POPULARITY_KEY_PREFIX + today, 1, msg.courseNo)
            jedis.expire(COURSE_POPULARITY_KEY_PREFIX + today, 172800)

          } else if (msg.`type` == "DROP") {
            jedis.decr(dailyCountKey)
            jedis.srem(dailyStudentsKey, msg.studentNo)

            jedis.zincrby(COURSE_POPULARITY_KEY_PREFIX + today, -1, msg.courseNo)
          }
        }

        jedis.close()
        println(s"[Batch $epochId] Processed ${messages.length} messages for date: $today")
      }
      .option("checkpointLocation", "/tmp/spark/checkpoint/realtime_stats")
      .outputMode("append")
      .start()

    query.awaitTermination()
  }
}