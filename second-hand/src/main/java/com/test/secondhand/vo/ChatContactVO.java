package com.test.secondhand.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天联系人视图对象
 */
@Data
public class ChatContactVO {

    private Long userId;
    private String nickname;
    private String avatar;
    private String lastMessage;
    private LocalDateTime lastMessageTime;
    private Integer unreadCount;
}
