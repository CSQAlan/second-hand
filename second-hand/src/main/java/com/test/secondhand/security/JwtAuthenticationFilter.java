package com.test.secondhand.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtils jwtUtils;

    @Value("${jwt.header}")
    private String tokenHeader;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String authHeader = request.getHeader(tokenHeader);
        String token = null;
        String username = null;
        Long userId = null;
        String role = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
            try {
                username = jwtUtils.getUsernameFromToken(token);
                userId = jwtUtils.getUserIdFromToken(token);
                role = jwtUtils.getRoleFromToken(token);
            } catch (Exception e) {
                logger.error("解析 JWT Token 失败: " + e.getMessage());
            }
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // 设置到 Spring Security 容器中，供标准角色校验使用
            SimpleGrantedAuthority authority = new SimpleGrantedAuthority(role);
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    username, null, Collections.singletonList(authority));
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // 同时放入自定义的 ThreadLocal UserContext 供高并发轻量鉴权使用
            UserContext.setUser(userId, username, role);
        } else if (username != null) {
            // 如果安全上下文已有认证，但 ThreadLocal 还没有（例如其他特殊情况），也放入 ThreadLocal
            UserContext.setUser(userId, username, role);
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            // 请求结束，务必清理 ThreadLocal，防止内存泄漏和线程复用污染
            UserContext.clear();
        }
    }
}
