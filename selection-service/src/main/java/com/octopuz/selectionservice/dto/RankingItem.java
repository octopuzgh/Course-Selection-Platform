package com.octopuz.selectionservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
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
