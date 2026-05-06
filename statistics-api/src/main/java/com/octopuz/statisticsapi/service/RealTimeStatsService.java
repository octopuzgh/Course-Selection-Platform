package com.octopuz.statisticsapi.service;

import com.octopuz.statisticsapi.dto.RankingItem;
import com.octopuz.statisticsapi.dto.TodayStats;
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
public class RealTimeStatsService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String RANKING_KEY = "course:ranking";
    private static final String STOCK_KEY_PREFIX = "course:stock:";
    private static final String CAPACITY_KEY_PREFIX = "course:capacity:";
    private static final String SELECTED_KEY_PREFIX = "selected:";

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

                RankingItem item = RankingItem.builder()
                        .courseNo(courseNo)
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

    public Long getTotalCount() {
        String count = redisTemplate.opsForValue().get("stats:total");
        return count != null ? Long.parseLong(count) : 0L;
    }

    public TodayStats getTodayStats() {
        String countStr = redisTemplate.opsForValue().get("stats:today:count");
        Long totalCount = countStr != null ? Long.parseLong(countStr) : 0L;

        Long uniqueStudents = redisTemplate.opsForSet().size("stats:today:students");

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

    private Integer getTotalCapacity(String courseNo) {
        String key = CAPACITY_KEY_PREFIX + courseNo;
        String capacityStr = redisTemplate.opsForValue().get(key);
        return capacityStr != null ? Integer.parseInt(capacityStr) : 0;
    }
}
