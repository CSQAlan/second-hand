package com.test.secondhand.vo;

import com.test.secondhand.entity.User;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户公开信息视图对象（仅包含公开字段）
 */
@Data
public class UserPublicVO {

    private Long id;
    private String nickname;
    private String avatar;
    private String bio;
    private LocalDateTime createTime;

    /**
     * 从 Entity 转换为 VO
     */
    public static UserPublicVO from(User user) {
        if (user == null) {
            return null;
        }
        UserPublicVO vo = new UserPublicVO();
        vo.setId(user.getId());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setBio(user.getBio());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }
}
