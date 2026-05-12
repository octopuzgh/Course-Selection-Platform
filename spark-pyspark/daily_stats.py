from pyspark.sql import SparkSession
from pyspark.sql.functions import count, countDistinct, to_date, date_format

from config import (
    MYSQL_HOST, MYSQL_PORT, MYSQL_DB, MYSQL_USER, MYSQL_PASSWORD,
    SPARK_CONFIG
)

JDBC_URL = f"jdbc:mysql://{MYSQL_HOST}:{MYSQL_PORT}/{MYSQL_DB}?useSSL=false&serverTimezone=Asia/Shanghai"
JDBC_PROPS = {
    "user": MYSQL_USER,
    "password": MYSQL_PASSWORD,
    "driver": "com.mysql.cj.jdbc.Driver",
}


def main():
    spark_builder = SparkSession.builder \
        .appName(SPARK_CONFIG["appName"]) \
        .master(SPARK_CONFIG["master"])

    for key, value in SPARK_CONFIG["config"].items():
        spark_builder = spark_builder.config(key, value)

    spark = spark_builder.getOrCreate()
    spark.sparkContext.setLogLevel("WARN")

    df = spark.read \
        .format("jdbc") \
        .option("url", JDBC_URL) \
        .options(**JDBC_PROPS) \
        .option("dbtable", "selection_record") \
        .load()

    df.createOrReplaceTempView("selection_record")

    daily_stats = spark.sql("""
        SELECT
            DATE(select_time) AS stat_date,
            COUNT(DISTINCT student_no) AS daily_students,
            COUNT(*) AS daily_selections,
            COUNT(DISTINCT course_no) AS daily_courses
        FROM selection_record
        GROUP BY DATE(select_time)
        ORDER BY stat_date DESC
    """)

    daily_stats = daily_stats.withColumn(
        "stat_date", 
        date_format(daily_stats["stat_date"], "yyyy-MM-dd")
    )

    daily_stats.write \
        .format("jdbc") \
        .option("url", JDBC_URL) \
        .options(**JDBC_PROPS) \
        .option("dbtable", "daily_stats") \
        .mode("overwrite") \
        .save()

    print(f"[DailyStats] Updated {daily_stats.count()} days")
    daily_stats.show(30, False)

    spark.stop()


if __name__ == "__main__":
    main()
