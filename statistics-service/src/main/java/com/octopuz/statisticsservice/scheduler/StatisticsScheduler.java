package com.octopuz.statisticsservice.scheduler;

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

    @Scheduled(cron = "0 0 0 * * ?")
    public void resetTodayStats() {
        log.info("开始清零今日统计");
        redisTemplate.delete("stats:today:count");
        redisTemplate.delete("stats:today:students");
        log.info("今日统计清零完成");
    }
}
