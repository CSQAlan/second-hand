package com.test.secondhand.config;

import com.test.secondhand.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 禁用 CSRF（因为是 RESTful API，不需要 Session 保护）
            .csrf(csrf -> csrf.disable())
            // 开启跨域
            .cors(cors -> cors.configure(http))
            // 统一配置请求拦截
            .authorizeHttpRequests(auth -> auth
                // 允许匿名访问静态上传图片文件
                .requestMatchers("/uploads/**").permitAll()
                // 用户注册与登录，放行
                .requestMatchers("/api/auth/**").permitAll()
                // 商品流式列表与详情，放行
                .requestMatchers("/api/goods/list", "/api/goods/detail/**", "/api/goods/search").permitAll()
                // 高并发秒杀下单与列表，放行（使用自定义 AOP 注解在业务层做轻量校验，避免 Security 滤镜瓶颈）
                .requestMatchers("/api/seckill/order", "/api/seckill/list", "/api/seckill/detail/**").permitAll()
                // 拍卖大厅信息，放行
                .requestMatchers("/api/auction/list", "/api/auction/detail/**").permitAll()
                // 智能客服，放行或需要登录（这里放行，方便演示）
                .requestMatchers("/api/chatbot/**").permitAll()
                // 用户公开信息，放行
                .requestMatchers("/api/user/*/public").permitAll()
                // 用户评价统计，放行
                .requestMatchers("/api/review/stats/**", "/api/review/user/**").permitAll()
                // 其余所有请求均需要登录认证
                .anyRequest().authenticated()
            )
            // 配置为无状态，不创建和使用 Session
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // 添加 JWT 拦截过滤器
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        // 允许的源，Vue 开发环境默认常用端口
        config.setAllowedOriginPatterns(Arrays.asList("http://localhost:*", "http://127.0.0.1:*"));
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
