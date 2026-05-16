package com.octopuz.selectionservice.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.octopuz.selectionservice.service.interf.RankingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RankingServiceImpl implements RankingService {

    @Autowired
    private StringRedisTemplate redisTemplate;
    private static final String RANKING_KEY = "course:ranking";

    @Override
    public void updateRanking(String courseNo) {
        redisTemplate.opsForZSet().incrementScore(RANKING_KEY, courseNo, -1);
        log.debug("课程{}排行榜更新，剩余数量-1", courseNo);
    }

    @Override
    public void restoreRanking(String courseNo) {
        redisTemplate.opsForZSet().incrementScore(RANKING_KEY, courseNo, 1);
        log.debug("课程{}排行榜恢复，剩余数量+1", courseNo);
    }

    @Override
    public void initCourseRanking(String courseNo, Integer remaining) {
        redisTemplate.opsForZSet().add(RANKING_KEY, courseNo, remaining.doubleValue());
        log.debug("课程{}排行榜初始化，剩余: {}", courseNo, remaining);
    }

    @Override
    public Long getRank(String courseNo) {
        Long rank = redisTemplate.opsForZSet().reverseRank(RANKING_KEY, courseNo);
        return rank != null ? rank + 1 : -1L;
    }
}
