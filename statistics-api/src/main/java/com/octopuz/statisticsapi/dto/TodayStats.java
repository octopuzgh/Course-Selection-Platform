package com.octopuz.statisticsapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TodayStats {
    private Long totalCount;
    private Long uniqueStudents;
}
