package com.campus.agent.config;

import com.campus.agent.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * 登录鉴权拦截器
 *
 * 拦截所有请求（排除路径除外），校验 JWT Token：
 * - API 请求（无 text/html 请求头）：返回 401 JSON
 * - 页面请求：重定向到 /login.html
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(AuthInterceptor.class);

    private final JwtUtil jwtUtil;

    public AuthInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {

        // 放行 OPTIONS 预检请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 从请求头获取 Token
        String authHeader = request.getHeader("Authorization");
        String token = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }

        // 如果请求参数中有 token 参数也尝试获取（兼容某些场景）
        if (token == null || token.isBlank()) {
            token = request.getParameter("token");
        }

        // 校验 Token
        if (token != null && !token.isBlank() && jwtUtil.validateToken(token)) {
            return true;
        }

        // Token 无效或不存在 — 判断是 API 请求还是页面请求
        String accept = request.getHeader("Accept");
        boolean isPageRequest = accept != null && accept.contains("text/html");

        if (isPageRequest) {
            // 页面请求 → 重定向到登录页
            log.debug("未登录页面请求，重定向到 /login.html: {}", request.getRequestURI());
            response.sendRedirect(request.getContextPath() + "/login.html");
        } else {
            // API 请求 → 返回 401 JSON
            log.debug("未登录 API 请求，返回 401: {}", request.getRequestURI());
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"code\":401,\"message\":\"未登录或 Token 已过期，请重新登录\"}");
        }

        return false;
    }
}
