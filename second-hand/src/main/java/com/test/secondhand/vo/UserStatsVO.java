package com.test.secondhand.vo;

import lombok.Data;

/**
 * 用户统计信息视图对象
 */
@Data
public class UserStatsVO {

    private Long publishCount;
    private Long sellCount;
    private Long buyCount;
    private Long reviewCount;
}
