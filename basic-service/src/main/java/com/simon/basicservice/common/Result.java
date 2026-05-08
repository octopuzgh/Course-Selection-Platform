package com.simon.basicservice.common;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {
    private int code;
    private String message;
    private T data;

    public static final int SUCCESS_CODE = 200;
    public static final int ERROR_CODE = 400;
    public static final int UNAUTHORIZED_CODE = 401;
    public static final int NOT_FOUND_CODE = 404;
    public static final int CONFLICT_CODE = 409;
    public static final int SERVER_ERROR_CODE = 500;

    public static <T> Result<T> success() {
        return new Result<>(SUCCESS_CODE, "成功", null);
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(SUCCESS_CODE, "成功", data);
    }

    public static <T> Result<T> success(String message, T data) {
        return new Result<>(SUCCESS_CODE, message, data);
    }

    public static <T> Result<T> error(int code, String message) {
        return new Result<>(code, message, null);
    }

    public static <T> Result<T> error(String message) {
        return new Result<>(ERROR_CODE, message, null);
    }

    public static <T> Result<T> serverError(String message) {
        return new Result<>(SERVER_ERROR_CODE, message, null);
    }
}
