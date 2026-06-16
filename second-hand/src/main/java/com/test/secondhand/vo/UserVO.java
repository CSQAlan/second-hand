package com.test.secondhand.vo;

import com.test.secondhand.entity.User;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户信息视图对象（脱敏，不含密码等敏感字段）
 */
@Data
public class UserVO {

    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private String phone;
    private String email;
    private Integer gender;
    private String address;
    private String bio;
    private String role;
    private LocalDateTime createTime;

    /**
     * 从 Entity 转换为 VO（脱敏处理）
     */
    public static UserVO from(User user) {
        if (user == null) {
            return null;
        }
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        // 手机号脱敏
        if (user.getPhone() != null && user.getPhone().length() == 11) {
            vo.setPhone(user.getPhone().substring(0, 3) + "****" + user.getPhone().substring(7));
        } else {
            vo.setPhone(user.getPhone());
        }
        vo.setEmail(user.getEmail());
        vo.setGender(user.getGender());
        vo.setAddress(user.getAddress());
        vo.setBio(user.getBio());
        vo.setRole(user.getRole());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }
}
