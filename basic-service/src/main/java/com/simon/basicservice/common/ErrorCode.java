package com.simon.basicservice.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    SUCCESS(200, "成功"),

    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),
    CONFLICT(409, "资源冲突"),

    SERVER_ERROR(500, "服务器内部错误"),

    USER_NOT_FOUND(1001, "用户不存在"),
    USER_PASSWORD_ERROR(1002, "密码错误"),
    USER_ALREADY_EXISTS(1003, "用户已存在"),

    STUDENT_NOT_FOUND(2001, "学生不存在"),
    STUDENT_ALREADY_EXISTS(2002, "学生已存在"),

    COURSE_NOT_FOUND(3001, "课程不存在"),
    COURSE_STOCK_INSUFFICIENT(3002, "课程名额不足"),
    COURSE_ALREADY_SELECTED(3003, "已选过该课程"),

    SELECTION_RECORD_NOT_FOUND(4001, "选课记录不存在"),

    BUSINESS_ERROR(5001, "业务处理失败"),

    PERMISSION_DENIED(5002, "权限不足");


    private final int code;
    private final String message;
}
