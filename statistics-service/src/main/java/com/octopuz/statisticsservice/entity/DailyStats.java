package com.octopuz.statisticsservice.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("daily_stats")
public class DailyStats {
    private String statDate;
    private Integer dailyStudents;
    private Integer dailySelections;
    private Integer selectCount;
    private Integer dropCount;
    private Integer dailyCourses;
}