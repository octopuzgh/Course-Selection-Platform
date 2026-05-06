package com.octopuz.statisticsapi.controller;

import com.octopuz.statisticsapi.dto.RankingItem;
import com.octopuz.statisticsapi.dto.TodayStats;
import com.octopuz.statisticsapi.service.RealTimeStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RealTimeStatsController {

    @Autowired
    private RealTimeStatsService realTimeStatsService;

    @GetMapping("/rank/top10")
    public ResponseEntity<List<RankingItem>> getTop10() {
        return ResponseEntity.ok(realTimeStatsService.getTop10());
    }

    @GetMapping("/rank/list")
    public ResponseEntity<List<RankingItem>> getRankingList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return ResponseEntity.ok(realTimeStatsService.getRankingList(page, size));
    }

    @GetMapping("/stats/total")
    public ResponseEntity<Long> getTotalCount() {
        return ResponseEntity.ok(realTimeStatsService.getTotalCount());
    }

    @GetMapping("/stats/today")
    public ResponseEntity<TodayStats> getTodayStats() {
        return ResponseEntity.ok(realTimeStatsService.getTodayStats());
    }

    @GetMapping("/course/{courseNo}/remaining")
    public ResponseEntity<Integer> getRemaining(@PathVariable String courseNo) {
        return ResponseEntity.ok(realTimeStatsService.getRemaining(courseNo));
    }

    @GetMapping("/check/selected")
    public ResponseEntity<Boolean> isSelected(
            @RequestParam String studentNo,
            @RequestParam String courseNo) {
        return ResponseEntity.ok(realTimeStatsService.isSelected(studentNo, courseNo));
    }
}
