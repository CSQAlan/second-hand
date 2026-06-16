package com.test.secondhand.vo;

import com.test.secondhand.entity.ChatMessage;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天消息视图对象
 */
@Data
public class ChatMessageVO {

    private Long id;
    private Long senderId;
    private Long receiverId;
    private Long goodsId;
    private String content;
    private Integer isRead;
    private LocalDateTime createTime;

    /**
     * 从 Entity 转换为 VO
     */
    public static ChatMessageVO from(ChatMessage message) {
        if (message == null) {
            return null;
        }
        ChatMessageVO vo = new ChatMessageVO();
        vo.setId(message.getId());
        vo.setSenderId(message.getSenderId());
        vo.setReceiverId(message.getReceiverId());
        vo.setGoodsId(message.getGoodsId());
        vo.setContent(message.getContent());
        vo.setIsRead(message.getIsRead());
        vo.setCreateTime(message.getCreateTime());
        return vo;
    }
}
