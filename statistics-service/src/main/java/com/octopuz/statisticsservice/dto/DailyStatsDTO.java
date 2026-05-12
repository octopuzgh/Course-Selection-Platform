package com.octopuz.statisticsservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyStatsDTO {
    private String statDate;
    private Long dailyCount;
    private Long dailyStudents;
}