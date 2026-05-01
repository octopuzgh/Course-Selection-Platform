package com.octopuz.selectionservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@TableName("selection_record")
public class SelectionRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String studentNo;

    private String courseNo;

    private LocalDateTime selectTime;
}
