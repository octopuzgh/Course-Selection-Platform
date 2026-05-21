package com.octopuz.selectionservice.interceptor;

import com.octopuz.selectionservice.context.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String operator = request.getHeader("X-Operator-Id");
        if (operator == null || operator.isEmpty()) {
            operator = request.getHeader("X-User-Id");
        }
        if (operator == null || operator.isEmpty()) {
            operator = "SYSTEM";
        }
        UserContext.setOperator(operator);
        log.debug("请求操作人: {}", operator);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}