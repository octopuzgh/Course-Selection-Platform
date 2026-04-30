package com.octopuz.selectionservice.service.impl;

import com.octopuz.selectionservice.client.BasicServiceClient;
import com.octopuz.selectionservice.dto.RankingItem;
import com.octopuz.selectionservice.service.interf.RankingService;
import com.octopuz.selectionservice.service.interf.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class RankingServiceImpl implements RankingService {

    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private StockService stockService;
    @Autowired
    private BasicServiceClient basicServiceClient;
    private static final String RANKING_KEY = "course:ranking";

    @Override
    public void updateRanking(String courseNo) {
        redisTemplate.opsForZSet().incrementScore(RANKING_KEY, courseNo, -1);
        log.debug("课程{}排行榜更新，剩余数量-1", courseNo);
    }

    @Override
    public void initCourseRanking(String courseNo, Integer remaining) {
        redisTemplate.opsForZSet().add(RANKING_KEY, courseNo, remaining.doubleValue());
        log.debug("课程{}排行榜初始化，剩余: {}", courseNo, remaining);
    }

    @Override
    public List<RankingItem> getAllCourses(Integer page, Integer size) {
        if (page == null || page < 1) {
            page = 1;
        }
        if (size == null || size < 1) {
            size = 10;
        }

        Long total = redisTemplate.opsForZSet().zCard(RANKING_KEY);
        if (total == null || total == 0) {
            return new ArrayList<>();
        }

        long start = (long) (page - 1) * size;
        long end = Math.min(start + size - 1, total - 1);

        Set<ZSetOperations.TypedTuple<String>> tuples = redisTemplate.opsForZSet()
                .reverseRangeWithScores(RANKING_KEY, start, end);

        List<RankingItem> ranking = new ArrayList<>();
        if (tuples != null) {
            int rank = (int) start + 1;
            for (ZSetOperations.TypedTuple<String> tuple : tuples) {
                String courseNo = tuple.getValue();
                Integer remainingCount = tuple.getScore() != null ? tuple.getScore().intValue() : 0;
                Integer totalCount = stockService.getTotalCapacity(courseNo);
                Integer selectedCount = totalCount - remainingCount;

                RankingItem item = new RankingItem();
                item.setCourseNo(courseNo);
                item.setCourseName(getCourseName(courseNo));
                item.setTotalCount(totalCount);
                item.setSelectedCount(selectedCount);
                item.setRemainingCount(remainingCount);
                item.setRank(rank++);
                ranking.add(item);
            }
        }
        return ranking;
    }

    @Override
    public Long getRank(String courseNo) {
        Long rank = redisTemplate.opsForZSet().reverseRank(RANKING_KEY, courseNo);
        return rank != null ? rank + 1 : -1L;
    }

    @Override
    public Long getScore(String courseNo) {
        Double score = redisTemplate.opsForZSet().score(RANKING_KEY, courseNo);
        return score != null ? score.longValue() : 0L;
    }

    private String getCourseName(String courseNo) {
        try {
            String courseJson = basicServiceClient.getCourse(courseNo);
            if (courseJson != null && courseJson.contains("\"course_name\":\"")) {
                int start = courseJson.indexOf("\"course_name\":\"") + 15;
                int end = courseJson.indexOf("\"", start);
                if (end > start) {
                    return courseJson.substring(start, end);
                }
            }
        } catch (Exception e) {
            log.warn("获取课程名称失败: {}", e.getMessage());
        }
        return "课程" + courseNo;
    }
}
