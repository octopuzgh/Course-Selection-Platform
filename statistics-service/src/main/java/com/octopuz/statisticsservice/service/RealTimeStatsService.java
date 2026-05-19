package com.octopuz.statisticsservice.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.octopuz.statisticsservice.client.BasicServiceClient;
import com.octopuz.statisticsservice.dto.DailyStatsDTO;
import com.octopuz.statisticsservice.dto.PopularityItem;
import com.octopuz.statisticsservice.dto.RankingItem;
import com.octopuz.statisticsservice.dto.TodayStats;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class RealTimeStatsService {

    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private BasicServiceClient basicServiceClient;

    private static final String RANKING_KEY = "course:ranking";
    private static final String STOCK_KEY_PREFIX = "course:stock:";
    private static final String CAPACITY_KEY_PREFIX = "course:capacity:";
    private static final String SELECTED_KEY_PREFIX = "selected:";
    private static final String STATS_TOTAL_KEY = "stats:total";
    private static final String STATS_TODAY_COUNT_KEY = "stats:today:count";
    private static final String STATS_TODAY_STUDENTS_KEY = "stats:today:students";
    private static final String DAILY_COUNT_KEY_PREFIX = "stats:daily:count:";
    private static final String DAILY_STUDENTS_KEY_PREFIX = "stats:daily:students:";
    private static final String COURSE_POPULARITY_KEY_PREFIX = "course:popularity:";

    public List<RankingItem> getTop10() {
        return getRankingList(1, 10);
    }

    public List<RankingItem> getRankingList(Integer page, Integer size) {
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
                Integer totalCount = getTotalCapacity(courseNo);
                Integer selectedCount = totalCount - remainingCount;
                String courseName = getCourseName(courseNo);

                RankingItem item = RankingItem.builder()
                        .courseNo(courseNo)
                        .courseName(courseName)
                        .totalCount(totalCount)
                        .selectedCount(selectedCount)
                        .remainingCount(remainingCount)
                        .rank(rank++)
                        .build();
                ranking.add(item);
            }
        }
        return ranking;
    }

    private String getCourseName(String courseNo) {
        try {
            String courseJson = basicServiceClient.getCourse(courseNo);
            if (courseJson != null) {
                JSONObject obj = JSON.parseObject(courseJson);
                if (obj != null && "200".equals(String.valueOf(obj.get("code")))) {
                    JSONObject data = obj.getJSONObject("data");
                    if (data != null) {
                        return data.getString("courseName");
                    }
                }
            }
        } catch (Exception e) {
            log.warn("获取课程{}名称失败: {}", courseNo, e.getMessage());
        }
        return null;
    }

    public Long getTotalCount() {
        String count = redisTemplate.opsForValue().get(STATS_TOTAL_KEY);
        return count != null ? Long.parseLong(count) : 0L;
    }

    public TodayStats getTodayStats() {
        String countStr = redisTemplate.opsForValue().get(STATS_TODAY_COUNT_KEY);
        Long totalCount = countStr != null ? Long.parseLong(countStr) : 0L;

        Long uniqueStudents = redisTemplate.opsForSet().size(STATS_TODAY_STUDENTS_KEY);

        return TodayStats.builder()
                .totalCount(totalCount)
                .uniqueStudents(uniqueStudents)
                .build();
    }

    public Integer getRemaining(String courseNo) {
        String key = STOCK_KEY_PREFIX + courseNo;
        String stockStr = redisTemplate.opsForValue().get(key);
        return stockStr != null ? Integer.parseInt(stockStr) : 0;
    }

    public boolean isSelected(String studentNo, String courseNo) {
        String key = SELECTED_KEY_PREFIX + studentNo + ":" + courseNo;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public DailyStatsDTO getDailyStats(String date) {
        String countKey = DAILY_COUNT_KEY_PREFIX + date;
        String studentsKey = DAILY_STUDENTS_KEY_PREFIX + date;

        String countStr = redisTemplate.opsForValue().get(countKey);
        Long dailyCount = countStr != null ? Long.parseLong(countStr) : 0L;

        Long dailyStudents = redisTemplate.opsForSet().size(studentsKey);

        return DailyStatsDTO.builder()
                .dailyCount(dailyCount)
                .dailyStudents(dailyStudents)
                .build();
    }

    public DailyStatsDTO getTodayDailyStats() {
        String today = LocalDate.now().toString();
        return getDailyStats(today);
    }

    public List<PopularityItem> getPopularityTop10() {
        return getPopularityRanking(1, 10);
    }

    public List<PopularityItem> getPopularityRanking(Integer page, Integer size) {
        return getPopularityRanking(LocalDate.now().toString(), page, size);
    }

    public List<PopularityItem> getPopularityRanking(String date, Integer page, Integer size) {
        if (page == null || page < 1) {
            page = 1;
        }
        if (size == null || size < 1) {
            size = 10;
        }

        String key = COURSE_POPULARITY_KEY_PREFIX + date;
        Long total = redisTemplate.opsForZSet().zCard(key);
        if (total == null || total == 0) {
            return new ArrayList<>();
        }

        long start = (long) (page - 1) * size;
        long end = Math.min(start + size - 1, total - 1);

        Set<ZSetOperations.TypedTuple<String>> tuples = redisTemplate.opsForZSet()
                .reverseRangeWithScores(key, start, end);

        List<PopularityItem> ranking = new ArrayList<>();
        if (tuples != null) {
            int rank = (int) start + 1;
            for (ZSetOperations.TypedTuple<String> tuple : tuples) {
                String courseNo = tuple.getValue();
                Integer selectionCount = tuple.getScore() != null ? tuple.getScore().intValue() : 0;

                PopularityItem item = PopularityItem.builder()
                        .courseNo(courseNo)
                        .selectionCount(selectionCount)
                        .rank(rank++)
                        .build();
                ranking.add(item);
            }
        }
        return ranking;
    }

    private Integer getTotalCapacity(String courseNo) {
        String key = CAPACITY_KEY_PREFIX + courseNo;
        String capacityStr = redisTemplate.opsForValue().get(key);
        return capacityStr != null ? Integer.parseInt(capacityStr) : 0;
    }
}