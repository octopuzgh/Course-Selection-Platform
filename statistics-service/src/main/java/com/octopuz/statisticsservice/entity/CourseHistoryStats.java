package com.octopuz.statisticsservice.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("course_history_stats")
public class CourseHistoryStats {
    private String courseNo;
    private String courseName;
    private Integer totalSelected;
    private Integer rank;
    private LocalDateTime lastUpdateTime;
}