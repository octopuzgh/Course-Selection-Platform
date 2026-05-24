package com.octopuz.selectionservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SelectionResponse {
    private Boolean success;
    private String message;
    private Long selectionId;

    public static SelectionResponse success(Long selectionId) {
        return new SelectionResponse(true, "选课成功", selectionId);
    }

    public static SelectionResponse successDrop(Long selectionId) {
        return new SelectionResponse(true, "退课成功", selectionId);
    }

    public static SelectionResponse fail(String message) {
        return new SelectionResponse(false, message, null);
    }
}
