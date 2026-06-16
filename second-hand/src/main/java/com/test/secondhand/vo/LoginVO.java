package com.test.secondhand.vo;

import lombok.Data;

/**
 * 登录返回视图对象
 */
@Data
public class LoginVO {

    private String token;
    private Long userId;
    private String username;
    private String nickname;
    private String role;
}
