package com.simon.basicservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("selection_record")
public class Selection_Record {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("student_no")
    private String studentNo;

    @TableField("course_no")
    private String courseNo;

    @TableField("select_time")
    private LocalDateTime selectTime;

}
