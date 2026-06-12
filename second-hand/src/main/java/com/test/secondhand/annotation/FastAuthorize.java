package com.test.secondhand.annotation;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface FastAuthorize {
    
    /**
     * 是否必须登录，默认为 true
     */
    boolean required() default true;

    /**
     * 限流时间窗口（单位：秒），0 表示不限流
     */
    int limitSeconds() default 0;

    /**
     * 在时间窗口内最大允许请求次数，0 表示不限流
     */
    int maxRequests() default 0;
}
