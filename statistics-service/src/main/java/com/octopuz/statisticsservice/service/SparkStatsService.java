package com.octopuz.statisticsservice.service;

import java.util.List;
import java.util.stream.Collectors;

import com.octopuz.statisticsservice.config.SparkProperties;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.octopuz.statisticsservice.entity.CourseHistoryStats;
import com.octopuz.statisticsservice.entity.DailyStats;
import com.octopuz.statisticsservice.mapper.CourseHistoryStatsMapper;
import com.octopuz.statisticsservice.mapper.DailyStatsMapper;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SparkStatsService {

    @Value("${spring.datasource.url}")
    private String jdbcUrl;

    @Value("${spring.datasource.username}")
    private String dbUser;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    @Value("${spark.app-name}")
    private String sparkAppName;

    @Value("${spark.master}")
    private String sparkMaster;

    @Value("${spark.driver-memory:512m}")
    private String driverMemory;

    @Value("${spark.sql-adaptive-enabled:true}")
    private boolean sqlAdaptiveEnabled;

    @Value("${spark.sql-adaptive-coalesce-partitions-enabled:true}")
    private boolean sqlAdaptiveCoalescePartitionsEnabled;

    @Autowired
    private CourseHistoryStatsMapper courseHistoryStatsMapper;

    @Autowired
    private DailyStatsMapper dailyStatsMapper;

    private SparkSession spark;

    @Autowired
    private SparkProperties sparkProperties;

    @PostConstruct
    public void init() {
        if (!sparkProperties.isEnabled()) {
            log.info("Spark is disabled in local development");
            return;
        }

        System.setProperty("hadoop.home.dir", "/tmp/hadoop");

        spark = SparkSession.builder()
                .appName(sparkProperties.getAppName())
                .master(sparkProperties.getMaster())
                .config("spark.driver.memory", sparkProperties.getDriverMemory())
                .config("spark.sql.adaptive.enabled", sparkProperties.isSqlAdaptiveEnabled())
                .config("spark.sql.adaptive.coalescePartitions.enabled", sparkProperties.isSqlAdaptiveCoalescePartitionsEnabled())
                .config("spark.ui.enabled", "false")
                .config("spark.ui.showConsoleProgress", "false")
                .getOrCreate();
        log.info("SparkSession initialized, version: {}", spark.version());
    }

    @PreDestroy
    public void destroy() {
        if (spark != null) {
            spark.stop();
            log.info("SparkSession stopped");
        }
    }

    public void calculateCourseHistoryStats() {
        log.info("Spark SQL: 开始计算课程历史统计");

        Dataset<Row> df = readSelectionRecord();
        df.createOrReplaceTempView("selection_record");

        Dataset<Row> result = spark.sql(
                "SELECT course_no, total_selected, " +
                "ROW_NUMBER() OVER (ORDER BY total_selected DESC) AS rank " +
                "FROM (" +
                "    SELECT course_no, COUNT(DISTINCT student_no) AS total_selected " +
                "    FROM selection_record " +
                "    WHERE action = 'SELECT' " +
                "    GROUP BY course_no" +
                ") t"
        );

        List<CourseHistoryStats> statsList = result.collectAsList().stream()
                .map(row -> CourseHistoryStats.builder()
                        .courseNo(row.getString(0))
                        .totalSelected((int) row.getLong(1))
                        .rank((int) row.getLong(2))
                        .build())
                .collect(Collectors.toList());

        courseHistoryStatsMapper.truncate();
        courseHistoryStatsMapper.batchInsert(statsList);

        log.info("Spark SQL: 课程历史统计完成, 共{}门课程", statsList.size());
    }

    public void calculateDailyStats() {
        log.info("Spark SQL: 开始计算每日统计");

        Dataset<Row> df = readSelectionRecord();
        df.createOrReplaceTempView("selection_record");

        Dataset<Row> result = spark.sql(
                "SELECT DATE(select_time) AS stat_date, " +
                "COUNT(*) AS total_count, " +
                "COUNT(DISTINCT student_no) AS unique_students " +
                "FROM selection_record " +
                "WHERE action = 'SELECT' " +
                "GROUP BY DATE(select_time) " +
                "ORDER BY stat_date"
        );

        List<DailyStats> statsList = result.collectAsList().stream()
                .map(row -> DailyStats.builder()
                        .statDate(row.getDate(0).toLocalDate())
                        .totalCount((int) row.getLong(1))
                        .uniqueStudents((int) row.getLong(2))
                        .build())
                .collect(Collectors.toList());

        for (DailyStats stats : statsList) {
            dailyStatsMapper.insert(stats);
        }

        log.info("Spark SQL: 每日统计完成, 共{}天数据", statsList.size());
    }

    private Dataset<Row> readSelectionRecord() {
        return spark.read()
                .format("jdbc")
                .option("url", jdbcUrl)
                .option("dbtable", "selection_record")
                .option("user", dbUser)
                .option("password", dbPassword)
                .option("driver", "com.mysql.cj.jdbc.Driver")
                .load();
    }
}