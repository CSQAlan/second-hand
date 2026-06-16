package com.test.secondhand.aop;

import com.test.secondhand.annotation.FastAuthorize;
import com.test.secondhand.exception.BusinessException;
import com.test.secondhand.security.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import org.aspectj.lang.reflect.MethodSignature;
import java.lang.reflect.Method;
import java.util.Collections;

@Aspect
@Component
public class FastAuthorizeAspect {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String LIMIT_LUA = 
            "local key = KEYS[1]\n" +
            "local now = tonumber(ARGV[1])\n" +
            "local window = tonumber(ARGV[2])\n" +
            "local max = tonumber(ARGV[3])\n" +
            "redis.call('zremrangebyscore', key, 0, now - window)\n" +
            "local current = redis.call('zcard', key)\n" +
            "if current < max then\n" +
            "    redis.call('zadd', key, now, now)\n" +
            "    redis.call('expire', key, math.ceil(window / 1000))\n" +
            "    return 1\n" +
            "else\n" +
            "    return 0\n" +
            "end";

    private final DefaultRedisScript<Long> limitScript;

    public FastAuthorizeAspect() {
        limitScript = new DefaultRedisScript<>();
        limitScript.setScriptText(LIMIT_LUA);
        limitScript.setResultType(Long.class);
    }

    @Before("@within(com.test.secondhand.annotation.FastAuthorize) || @annotation(com.test.secondhand.annotation.FastAuthorize)")
    public void doBefore(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        FastAuthorize fastAuthorize = method.getAnnotation(FastAuthorize.class);
        if (fastAuthorize == null) {
            fastAuthorize = method.getDeclaringClass().getAnnotation(FastAuthorize.class);
        }

        if (fastAuthorize == null) {
            return;
        }

        // 1. 登录校验
        if (fastAuthorize.required()) {
            if (UserContext.getUser() == null) {
                throw new BusinessException(401, "请先登录");
            }
        }

        // 2. 限流校验
        if (fastAuthorize.limitSeconds() > 0 && fastAuthorize.maxRequests() > 0) {
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
            String key;
            if (UserContext.getUser() != null) {
                key = "ratelimit:" + UserContext.getUserId() + ":" + request.getRequestURI();
            } else {
                key = "ratelimit:" + getIpAddr(request) + ":" + request.getRequestURI();
            }

            long now = System.currentTimeMillis();
            long window = fastAuthorize.limitSeconds() * 1000L;
            long max = fastAuthorize.maxRequests();

            Long result = redisTemplate.execute(limitScript, Collections.singletonList(key), 
                    String.valueOf(now), String.valueOf(window), String.valueOf(max));

            if (result == null || result == 0) {
                throw new BusinessException(429, "操作过于频繁，请稍后再试");
            }
        }
    }

    private String getIpAddr(HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
