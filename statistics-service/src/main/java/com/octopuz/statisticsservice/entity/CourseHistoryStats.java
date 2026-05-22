package com.octopuz.statisticsservice.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("course_history_stats")
public class CourseHistoryStats {
    private String courseNo;
    private Integer totalSelected;
    private Integer totalRecords;
    private Integer selectCount;
    private Integer dropCount;
    private String firstSelectTime;
    private String lastSelectTime;
    @TableField("`rank`")
    private Integer rank;
}