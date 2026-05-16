from pyspark.sql import SparkSession
from pyspark.sql.functions import count_distinct, row_number, date_format, col
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
        .option("dbtable", "selection_log") \
        .load()

    df.createOrReplaceTempView("selection_log")

    course_stats = spark.sql("""
        SELECT
            course_no,
            COUNT(DISTINCT CASE WHEN action = 'SELECT' THEN student_no END) AS select_students,
            COUNT(DISTINCT CASE WHEN action = 'DROP' THEN student_no END) AS drop_students,
            COUNT(CASE WHEN action = 'SELECT' THEN 1 END) AS select_count,
            COUNT(CASE WHEN action = 'DROP' THEN 1 END) AS drop_count,
            MIN(operate_time) AS first_select_time,
            MAX(operate_time) AS last_select_time
        FROM selection_log
        GROUP BY course_no
    """)

    course_stats = course_stats.withColumn(
        "total_selected",
        col("select_students") - col("drop_students")
    )
    course_stats = course_stats.withColumn(
        "total_records",
        col("select_count") + col("drop_count")
    )

    window_spec = Window.orderBy(col("total_selected").desc())
    ranked = course_stats.withColumn("rank", row_number().over(window_spec))

    ranked = ranked.withColumn("first_select_time", date_format(ranked["first_select_time"], "yyyy-MM-dd HH:mm:ss"))
    ranked = ranked.withColumn("last_select_time", date_format(ranked["last_select_time"], "yyyy-MM-dd HH:mm:ss"))

    result = ranked.select("course_no", "total_selected", "total_records",
                           "select_count", "drop_count",
                           "first_select_time", "last_select_time", "rank")

    result.write \
        .format("jdbc") \
        .option("url", JDBC_URL) \
        .options(**JDBC_PROPS) \
        .option("dbtable", "course_history_stats") \
        .option("timestampFormat", "yyyy-MM-dd HH:mm:ss") \
        .mode("overwrite") \
        .save()

    print(f"[CourseHistoryStats] Updated {result.count()} courses")
    result.show(10, False)

    spark.stop()


if __name__ == "__main__":
    main()