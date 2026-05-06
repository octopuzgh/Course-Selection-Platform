package com.octopuz.statisticsapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RankingItem {
    private String courseNo;
    private String courseName;
    private Integer totalCount;
    private Integer selectedCount;
    private Integer remainingCount;
    private Integer rank;
}
