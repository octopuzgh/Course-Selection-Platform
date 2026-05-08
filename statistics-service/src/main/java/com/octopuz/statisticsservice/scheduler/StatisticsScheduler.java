package com.octopuz.statisticsservice.scheduler;

import com.octopuz.statisticsservice.service.SparkStatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StatisticsScheduler {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private SparkStatsService sparkStatsService;

    @Scheduled(cron = "0 0 0 * * ?")
    public void resetTodayStats() {
        log.info("开始清零今日统计");
        redisTemplate.delete("stats:today:count");
        redisTemplate.delete("stats:today:students");
        log.info("今日统计清零完成");
    }

    @Scheduled(cron = "0 0 2 * * ?")
    public void updateStatisticsTables() {
        log.info("========== 开始离线统计（Spark SQL）==========");
        sparkStatsService.calculateCourseHistoryStats();
        sparkStatsService.calculateDailyStats();
        log.info("========== 离线统计完成 ==========");
    }
}
