package com.simon.basicservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentSelection {
    private String courseNo;
    private String courseName;
    private String teacher;
    private BigDecimal credit;
    private Integer totalCapacity;
    private Integer remaining;
    private LocalDateTime selectTime;
}
