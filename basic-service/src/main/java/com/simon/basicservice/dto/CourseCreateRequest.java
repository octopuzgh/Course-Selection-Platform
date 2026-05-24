package com.simon.basicservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CourseCreateRequest {

    @NotBlank(message = "课程号不能为空")
    @Pattern(regexp = "^[A-Z][A-Z0-9]*$", message = "课程号必须由大写字母和数字组成，且必须以一个大写字母开头")
    private String courseNo;

    @NotBlank(message = "课程名称不能为空")
    private String courseName;

    @NotBlank(message = "教师不能为空")
    private String teacher;

    @NotNull(message = "学分不能为空")
    private BigDecimal credit;

    @NotNull(message = "总容量不能为空")
    private Integer totalCapacity;

    @NotNull(message = "剩余名额不能为空")
    private Integer remaining;

    @NotNull(message = "已选人数不能为空")
    private Integer selectedCount;
}
