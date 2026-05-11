from pyspark.sql import SparkSession

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
        .option("dbtable", "selection_record") \
        .option("user", MYSQL_USER) \
        .option("password", MYSQL_PASSWORD) \
        .option("driver", "com.mysql.cj.jdbc.Driver") \
        .load()

    df.createOrReplaceTempView("selection_record")

    # 使用 Spark SQL 统计课程选课人数
    course_stats = spark.sql("""
        SELECT 
            course_no,
            COUNT(DISTINCT student_no) AS total_selected
        FROM selection_record
        GROUP BY course_no
    """)

    course_stats.createOrReplaceTempView("course_stats")

    # 使用 Spark SQL 排名
    ranked = spark.sql("""
        SELECT 
            course_no,
            total_selected,
            ROW_NUMBER() OVER (ORDER BY total_selected DESC) AS rank
        FROM course_stats
    """)

    ranked.write \
        .format("jdbc") \
        .option("url", JDBC_URL) \
        .option("dbtable", "course_history_stats") \
        .option("user", MYSQL_USER) \
        .option("password", MYSQL_PASSWORD) \
        .option("driver", "com.mysql.cj.jdbc.Driver") \
        .mode("overwrite") \
        .save()

    print(f"[CourseHistoryStats] Updated {ranked.count()} courses")

    spark.stop()


if __name__ == "__main__":
    main()