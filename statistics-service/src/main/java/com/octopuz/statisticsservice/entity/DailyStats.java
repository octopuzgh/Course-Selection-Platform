package com.octopuz.statisticsservice.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("daily_stats")
public class DailyStats {
    private LocalDate statDate;
    private Integer dailyStudents;
    private Integer dailySelections;
    private Integer dailyCourses;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}