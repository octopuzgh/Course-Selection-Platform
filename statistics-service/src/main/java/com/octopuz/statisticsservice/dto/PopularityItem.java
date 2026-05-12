package com.octopuz.statisticsservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PopularityItem {
    private String courseNo;
    private Integer selectionCount;
    private Integer rank;
}