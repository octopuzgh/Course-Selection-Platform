package com.octopuz.selectionservice.exception;

import com.octopuz.selectionservice.dto.SelectionResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.annotation.ResponseStatus;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<SelectionResponse> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        SelectionResponse response = SelectionResponse.fail(e.getMessage());
        return ResponseEntity.status(e.getCode()).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<SelectionResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("参数异常: {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(SelectionResponse.fail("参数错误: " + e.getMessage()));
    }

    @ExceptionHandler(NullPointerException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<SelectionResponse> handleNullPointerException(NullPointerException e) {
        log.error("空指针异常", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(SelectionResponse.fail("服务器内部错误"));
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<SelectionResponse> handleException(Exception e) {
        log.error("未知异常", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(SelectionResponse.fail("服务器内部错误: " + e.getMessage()));
    }
}
