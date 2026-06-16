package com.test.secondhand.service;

import java.util.Map;

/**
 * 认证服务接口
 */
public interface AuthService {

    /**
     * 用户注册
     */
    void register(String username, String password, String nickname);

    /**
     * 用户登录
     * @return 返回包含 token, username, userId, role 的 map
     */
    Map<String, Object> login(String username, String password);
}
