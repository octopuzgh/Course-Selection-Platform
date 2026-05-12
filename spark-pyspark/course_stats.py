from pyspark.sql import SparkSession
from pyspark.sql.functions import count_distinct, row_number, to_date, date_format
from pyspark.sql.window import Window

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

    course_stats = spark.sql("""
        SELECT
            course_no,
            COUNT(DISTINCT student_no) AS total_selected,
            COUNT(*) AS total_records,
            MIN(select_time) AS first_select_time,
            MAX(select_time) AS last_select_time
        FROM selection_record
        GROUP BY course_no
    """)

    window_spec = Window.orderBy(course_stats["total_selected"].desc())
    ranked = course_stats.withColumn("rank", row_number().over(window_spec))

    ranked = ranked.withColumn("first_select_time", date_format(ranked["first_select_time"], "yyyy-MM-dd HH:mm:ss"))
    ranked = ranked.withColumn("last_select_time", date_format(ranked["last_select_time"], "yyyy-MM-dd HH:mm:ss"))

    ranked.write \
        .format("jdbc") \
        .option("url", JDBC_URL) \
        .options(**JDBC_PROPS) \
        .option("dbtable", "course_history_stats") \
        .option("timestampFormat", "yyyy-MM-dd HH:mm:ss") \
        .mode("overwrite") \
        .save()

    print(f"[CourseHistoryStats] Updated {ranked.count()} courses")
    ranked.show(10, False)

    spark.stop()


if __name__ == "__main__":
    main()
