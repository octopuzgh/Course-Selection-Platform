package com.octopuz.statisticsservice.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.octopuz.statisticsservice.entity.CourseHistoryStats;
import com.octopuz.statisticsservice.entity.DailyStats;
import com.octopuz.statisticsservice.mapper.CourseHistoryStatsMapper;
import com.octopuz.statisticsservice.mapper.DailyStatsMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OfflineStatsService {

    @Autowired
    private CourseHistoryStatsMapper courseHistoryStatsMapper;

    @Autowired
    private DailyStatsMapper dailyStatsMapper;

    public List<CourseHistoryStats> getCourseRanking(Integer limit) {
        if (limit == null || limit < 1) {
            limit = 10;
        }
        LambdaQueryWrapper<CourseHistoryStats> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(CourseHistoryStats::getRank).last("LIMIT " + limit);
        return courseHistoryStatsMapper.selectList(wrapper);
    }

    public CourseHistoryStats getCourseTotal(String courseNo) {
        LambdaQueryWrapper<CourseHistoryStats> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CourseHistoryStats::getCourseNo, courseNo);
        return courseHistoryStatsMapper.selectOne(wrapper);
    }

    public List<DailyStats> getDailyStats(LocalDate start, LocalDate end) {
        String startStr = start.toString();
        String endStr = end.toString();
        LambdaQueryWrapper<DailyStats> wrapper = new LambdaQueryWrapper<>();
        wrapper.between(DailyStats::getStatDate, startStr, endStr)
                .orderByAsc(DailyStats::getStatDate);
        return dailyStatsMapper.selectList(wrapper);
    }

    public Map<String, Object> getSummary() {
        LambdaQueryWrapper<DailyStats> wrapper = new LambdaQueryWrapper<>();
        List<DailyStats> allStats = dailyStatsMapper.selectList(wrapper);

        long totalSelections = allStats.stream().mapToLong(DailyStats::getDailySelections).sum();
        long totalStudents = allStats.stream().mapToLong(DailyStats::getDailyStudents).sum();
        long totalSelectCount = allStats.stream().mapToLong(DailyStats::getSelectCount).sum();
        long totalDropCount = allStats.stream().mapToLong(DailyStats::getDropCount).sum();

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalSelections", totalSelections);
        summary.put("totalSelectCount", totalSelectCount);
        summary.put("totalDropCount", totalDropCount);
        summary.put("totalStudents", totalStudents);
        summary.put("totalDays", allStats.size());
        return summary;
    }
}