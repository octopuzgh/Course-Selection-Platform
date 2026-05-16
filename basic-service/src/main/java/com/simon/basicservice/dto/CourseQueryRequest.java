package com.simon.basicservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CourseQueryRequest {

    @NotBlank(message = "课程号不能为空")
    @Pattern(regexp = "^[A-Z]{2,4}\\d{3,4}$", message = "课程号格式不正确，如：CS101")
    private String courseNo;
}
