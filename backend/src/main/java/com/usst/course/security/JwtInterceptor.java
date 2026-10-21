package com.usst.course.security;

import com.usst.course.common.BizException;
import com.usst.course.common.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;

/**
 * JWT 解析 + 角色校验。解析结果放入 {@link UserContext}，请求结束时清理。
 */
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        if (!(handler instanceof HandlerMethod method)) {
            return true;
        }

        String header = request.getHeader(HEADER);
        if (header == null || !header.startsWith(PREFIX)) {
            throw BizException.of(ErrorCode.UNAUTHORIZED);
        }
        LoginUser user = jwtUtil.parse(header.substring(PREFIX.length()).trim());
        UserContext.set(user);

        RequireRole required = method.getMethodAnnotation(RequireRole.class);
        if (required == null) {
            required = method.getBeanType().getAnnotation(RequireRole.class);
        }
        if (required != null && Arrays.stream(required.value()).noneMatch(r -> r.equals(user.role()))) {
            throw BizException.of(ErrorCode.FORBIDDEN);
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContext.clear();
    }
}
