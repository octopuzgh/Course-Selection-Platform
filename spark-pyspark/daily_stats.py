from pyspark.sql import SparkSession
import os

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
    # 设置本地 IP 避免网络警告
    os.environ['SPARK_LOCAL_IP'] = '192.168.152.131'

    spark_builder = SparkSession.builder \
        .appName(SPARK_CONFIG["appName"]) \
        .master(SPARK_CONFIG["master"])

    for key, value in SPARK_CONFIG["config"].items():
        spark_builder = spark_builder.config(key, value)

    spark = spark_builder.getOrCreate()

    spark.sparkContext.setLogLevel("WARN")

    # 读取选课记录 - 使用统一的 JDBC 配置
    df = spark.read \
        .format("jdbc") \
        .option("url", JDBC_URL) \
        .options(**JDBC_PROPS) \
        .option("dbtable", "selection_record") \
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

    # 优化：设置 shuffle 分区数为 1，避免窗口函数警告
    spark.sql("SET spark.sql.shuffle.partitions=1")

    # 使用 Spark SQL 排名
    ranked = spark.sql("""
        SELECT 
            course_no,
            total_selected,
            ROW_NUMBER() OVER (ORDER BY total_selected DESC) AS rank
        FROM course_stats
    """)

    # 先统计数量（触发 action）
    course_count = ranked.count()

    # 写入结果 - 使用统一的 JDBC 配置
    ranked.write \
        .format("jdbc") \
        .option("url", JDBC_URL) \
        .options(**JDBC_PROPS) \
        .option("dbtable", "course_history_stats") \
        .mode("overwrite") \
        .save()

    print(f"[CourseHistoryStats] Successfully updated {course_count} courses")
    if course_count > 0:
        print("[CourseHistoryStats] Top 5 courses:")
        ranked.show(5)

    spark.stop()


if __name__ == "__main__":
    main()
