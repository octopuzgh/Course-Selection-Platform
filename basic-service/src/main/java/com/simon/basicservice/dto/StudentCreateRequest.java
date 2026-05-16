package com.simon.basicservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "学生创建请求")
public class StudentCreateRequest {

    @NotBlank(message = "学号不能为空")
    @Pattern(regexp = "^\\d{8,12}$", message = "学号格式不正确，应为8-12位数字")
    @Schema(description = "学号", example = "20240001", requiredMode = Schema.RequiredMode.REQUIRED)
    private String studentNo;

    @NotBlank(message = "姓名不能为空")
    @Size(min = 1, max = 50, message = "姓名长度应在1-50个字符之间")
    @Schema(description = "姓名", example = "张三", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @NotBlank(message = "专业不能为空")
    @Size(min = 1, max = 100, message = "专业名称长度应在1-100个字符之间")
    @Schema(description = "专业", example = "计算机科学与技术", requiredMode = Schema.RequiredMode.REQUIRED)
    private String major;

    @NotNull(message = "年级不能为空")
    @Min(value = 2020, message = "年级不能早于2020年")
    @Max(value = 2030, message = "年级不能晚于2030年")
    @Schema(description = "年级", example = "2024", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer grade;

}
