# PySpark Batch Processing Configuration
import os
from dotenv import load_dotenv

# 加载环境变量
load_dotenv("../../.env")

MYSQL_HOST = os.getenv("DB_HOST", "192.168.152.131")
MYSQL_PORT = int(os.getenv("DB_PORT", 3306))
MYSQL_DB = os.getenv("DB_NAME", "student_class")
MYSQL_USER = os.getenv("DB_USERNAME", "octopuz_remote")
MYSQL_PASSWORD = os.getenv("DB_PASSWORD", "Octopuz@123")

REDIS_HOST = os.getenv("REDIS_HOST", "192.168.152.131")
REDIS_PORT = int(os.getenv("REDIS_PORT", 6379))
REDIS_DB = int(os.getenv("REDIS_DATABASE", 1))
REDIS_PASSWORD = os.getenv("REDIS_PASSWORD", "Octopuz@123")

SPARK_CONFIG = {
    "appName": "绩效统计API",
    "master": "local[*]",
    "config": {
        "spark.sql.adaptive.enabled": "true",
        "spark.sql.adaptive.coalescePartitions.enabled": "true",
        "spark.jars": "/opt/spark-4.1.1-bin-hadoop3/jars/mysql-connector-j-8.0.33.jar"
    }
}
