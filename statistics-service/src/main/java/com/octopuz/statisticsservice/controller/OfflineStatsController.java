package com.octopuz.statisticsservice.controller;

import com.octopuz.statisticsservice.entity.CourseHistoryStats;
import com.octopuz.statisticsservice.entity.DailyStats;
import com.octopuz.statisticsservice.service.OfflineStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class OfflineStatsController {

    @Autowired
    private OfflineStatsService offlineStatsService;

    @GetMapping("/course/ranking")
    public ResponseEntity<List<CourseHistoryStats>> getCourseRanking(
            @RequestParam(defaultValue = "10") Integer limit) {
        return ResponseEntity.ok(offlineStatsService.getCourseRanking(limit));
    }

    @GetMapping("/course/total")
    public ResponseEntity<CourseHistoryStats> getCourseTotal(
            @RequestParam String courseNo) {
        return ResponseEntity.ok(offlineStatsService.getCourseTotal(courseNo));
    }

    @GetMapping("/daily")
    public ResponseEntity<List<DailyStats>> getDailyStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(offlineStatsService.getDailyStats(start, end));
    }

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary() {
        return ResponseEntity.ok(offlineStatsService.getSummary());
    }
}