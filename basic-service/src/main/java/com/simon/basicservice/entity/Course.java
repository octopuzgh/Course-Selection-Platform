package com.simon.basicservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("course")
public class Course {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("course_no")
    private String courseNo;

    @TableField("course_name")
    private String courseName;

    private String teacher;

    private BigDecimal credit;   // 对应 decimal(3,1)

    @TableField("total_capacity")
    private Integer totalCapacity;

    private Integer remaining;

    @TableField("selected_count")
    private Integer selectedCount;

}
