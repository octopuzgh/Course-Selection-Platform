package com.simon.basicservice.entity;

import com.baomidou.mybatisplus.annotation.*;
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

    @TableField(value="select_time", fill = FieldFill.INSERT)
    private LocalDateTime selectTime;

}
