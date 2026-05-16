package com.simon.basicservice.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.simon.basicservice.annotation.RequireRole;
import com.simon.basicservice.common.ErrorCode;
import com.simon.basicservice.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final String USER_ROLE_HEADER = "X-User-Role";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        RequireRole requireRole = handlerMethod.getMethodAnnotation(RequireRole.class);

        if (requireRole == null) {
            return true;
        }

        String userRole = request.getHeader(USER_ROLE_HEADER);

        if (userRole == null || userRole.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(objectMapper.writeValueAsString(
                    Result.error(ErrorCode.UNAUTHORIZED)
            ));
            return false;
        }

        String[] allowedRoles = requireRole.value();
        boolean hasPermission = false;
        for (String role : allowedRoles) {
            if (role.equals(userRole)) {
                hasPermission = true;
                break;
            }
        }

        if (!hasPermission) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(objectMapper.writeValueAsString(
                    Result.error(ErrorCode.PERMISSION_DENIED)
            ));
            return false;
        }

        return true;
    }
}
