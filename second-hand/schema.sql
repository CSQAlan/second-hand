-- 创建数据库 (如果不存在)
CREATE DATABASE IF NOT EXISTS `second_hand` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE `second_hand`;

-- 1. 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT AUTO_INCREMENT COMMENT '主键ID',
    `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    `password` VARCHAR(100) NOT NULL COMMENT '加密密码',
    `nickname` VARCHAR(50) COMMENT '昵称',
    `avatar` VARCHAR(255) COMMENT '头像',
    `phone` VARCHAR(20) COMMENT '手机号',
    `email` VARCHAR(100) COMMENT '邮箱',
    `gender` TINYINT DEFAULT 0 COMMENT '性别: 0-未知, 1-男, 2-女',
    `address` VARCHAR(500) COMMENT '地址',
    `bio` VARCHAR(500) COMMENT '个人简介',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 0-禁用, 1-正常',
    `role` VARCHAR(20) DEFAULT 'ROLE_USER' COMMENT '角色：ROLE_USER, ROLE_ADMIN',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 2. 常规商品表
CREATE TABLE IF NOT EXISTS `goods` (
    `id` BIGINT AUTO_INCREMENT COMMENT '商品ID',
    `name` VARCHAR(100) NOT NULL COMMENT '商品名称',
    `description` TEXT COMMENT '商品描述',
    `price` DECIMAL(10, 2) NOT NULL COMMENT '价格',
    `image_url` VARCHAR(255) COMMENT '主图URL',
    `images` TEXT COMMENT '多图JSON数组',
    `category` VARCHAR(50) COMMENT '分类',
    `condition` VARCHAR(50) COMMENT '成色: 全新/九成新/八成新等',
    `trading_method` VARCHAR(100) COMMENT '交易方式: 面交/邮寄',
    `location` VARCHAR(200) COMMENT '交易地点',
    `view_count` INT DEFAULT 0 COMMENT '浏览量',
    `status` INT DEFAULT 0 COMMENT '状态：0-在售, 1-已售, 2-下架',
    `seller_id` BIGINT NOT NULL COMMENT '卖家ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX idx_seller (`seller_id`),
    INDEX idx_status (`status`),
    INDEX idx_category (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='二手商品表';

-- 3. 秒杀/抢购商品表
CREATE TABLE IF NOT EXISTS `seckill_goods` (
    `id` BIGINT AUTO_INCREMENT COMMENT '秒杀商品ID',
    `goods_id` BIGINT NOT NULL COMMENT '关联商品ID',
    `seckill_price` DECIMAL(10, 2) NOT NULL COMMENT '秒杀价格',
    `stock` INT NOT NULL COMMENT '秒杀库存',
    `start_time` DATETIME NOT NULL COMMENT '秒杀开始时间',
    `end_time` DATETIME NOT NULL COMMENT '秒杀结束时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY ukey_goods (`goods_id`),
    INDEX idx_seckill_time (`start_time`, `end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀商品表';

-- 4. 拍卖商品表
CREATE TABLE IF NOT EXISTS `auction_goods` (
    `id` BIGINT AUTO_INCREMENT COMMENT '拍卖商品ID',
    `goods_id` BIGINT NOT NULL COMMENT '关联商品ID',
    `start_price` DECIMAL(10, 2) NOT NULL COMMENT '起拍价',
    `current_price` DECIMAL(10, 2) NOT NULL COMMENT '当前最高出价',
    `highest_bidder_id` BIGINT DEFAULT NULL COMMENT '当前最高出价者ID',
    `min_increment` DECIMAL(10, 2) NOT NULL COMMENT '最小加价幅度',
    `start_time` DATETIME NOT NULL COMMENT '拍卖开始时间',
    `end_time` DATETIME NOT NULL COMMENT '拍卖结束时间',
    `status` INT DEFAULT 0 COMMENT '状态：0-竞拍中, 1-已结束',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY ukey_goods (`goods_id`),
    INDEX idx_auction_time (`start_time`, `end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='拍卖商品表';

-- 5. 订单表
CREATE TABLE IF NOT EXISTS `orders` (
    `id` BIGINT AUTO_INCREMENT COMMENT '订单ID',
    `order_no` VARCHAR(64) NOT NULL UNIQUE COMMENT '订单编号',
    `goods_id` BIGINT NOT NULL COMMENT '商品ID',
    `buyer_id` BIGINT NOT NULL COMMENT '买家ID',
    `seller_id` BIGINT NOT NULL COMMENT '卖家ID',
    `price` DECIMAL(10, 2) NOT NULL COMMENT '订单成交价格',
    `type` INT DEFAULT 0 COMMENT '订单类型：0-普通购买, 1-秒杀抢购, 2-拍卖成交',
    `status` INT DEFAULT 0 COMMENT '订单状态：0-待付款, 1-已付款, 2-已发货, 3-已收货, 4-交易成功, 5-已取消',
    `delivery_no` VARCHAR(100) DEFAULT NULL COMMENT '物流单号 / 发货说明',
    `pay_time` DATETIME COMMENT '付款时间',
    `ship_time` DATETIME COMMENT '发货时间',
    `receive_time` DATETIME COMMENT '收货时间',
    `cancel_time` DATETIME COMMENT '取消时间',
    `cancel_reason` VARCHAR(500) COMMENT '取消原因',
    `remark` VARCHAR(500) COMMENT '备注',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX idx_buyer (`buyer_id`),
    INDEX idx_seller (`seller_id`),
    INDEX idx_order_no (`order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- 6. 拍卖出价记录表
CREATE TABLE IF NOT EXISTS `auction_record` (
    `id` BIGINT AUTO_INCREMENT COMMENT '出价记录ID',
    `auction_goods_id` BIGINT NOT NULL COMMENT '关联拍卖商品ID',
    `bidder_id` BIGINT NOT NULL COMMENT '出价者ID',
    `bid_price` DECIMAL(10, 2) NOT NULL COMMENT '出价金额',
    `bid_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '出价时间',
    PRIMARY KEY (`id`),
    INDEX idx_auction_goods (`auction_goods_id`),
    INDEX idx_bidder (`bidder_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='拍卖出价记录表';

-- 7. 智能客服问答库（本地RAG知识库数据）
CREATE TABLE IF NOT EXISTS `knowledge_base` (
    `id` BIGINT AUTO_INCREMENT COMMENT '主键ID',
    `question` VARCHAR(255) NOT NULL COMMENT '常见提问',
    `answer` TEXT NOT NULL COMMENT '官方解答内容',
    `category` VARCHAR(50) DEFAULT 'general' COMMENT '分类：trade, seckill, auction, general',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能客服本地知识库';

-- 8. 图片/文件上传历史表
CREATE TABLE IF NOT EXISTS `upload_file` (
    `id` BIGINT AUTO_INCREMENT COMMENT '主键ID',
    `filename` VARCHAR(100) NOT NULL COMMENT '原始文件名',
    `file_path` VARCHAR(255) NOT NULL UNIQUE COMMENT '本地存储文件相对/绝对路径',
    `is_used` TINYINT DEFAULT 0 COMMENT '是否已使用：0-未使用, 1-已使用',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX idx_is_used (`is_used`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图片文件上传跟踪表';

-- 插入一些初始的知识库数据以供 RAG 客服使用
INSERT INTO `knowledge_base` (`question`, `answer`, `category`) VALUES
('系统如何保证秒杀抢购的公平性？', '我们的系统使用 Redis + Lua 脚本进行原子扣减库存，能绝对防止超卖，并且同一用户只能购买一次。每个请求都会经过轻量级 AOP 限流拦截，防止刷单软件。', 'seckill'),
('商品抢到了能反悔吗？订单多长时间内必须支付？', '秒杀成功后，订单的默认状态为待付款。请在 15 分钟内完成付款，超时后系统将自动取消订单并将商品库存退回库中。', 'trade'),
('拍卖是如何竞价和成交的？', '在拍卖结束前，您可以随时出价，但出价金额必须比当前最高出价高出至少“最小加价幅度”。拍卖倒计时结束时，出价最高的用户即成功拍得商品，系统将自动生成拍卖订单。', 'auction'),
('客服怎么联系？二手交易如何发货？', '卖家可以选择邮寄或线下自提。在商品售出后，卖家可以联系客服进行物流单号登记，或者与买家在站内信/本智能客服中沟通线下自提的时间地点。', 'general');

-- 插入默认测试用户账号数据
INSERT INTO `user` (`id`, `username`, `password`, `nickname`, `role`) VALUES
(1, 'admin', '$2a$10$Los6KfWUrXcAogOsIY.Hh.LjfOsAEef.6TLHl1YsLH8L1DqptZXN6', '系统超级管理员', 'ROLE_ADMIN'),
(2, 'seller', '$2a$10$nNan3gNsfalNsnBOEjG.7.AppDqHijK0Z.SmgTdB3DRiGlniRC4yu', '二手闲置卖家', 'ROLE_USER'),
(3, 'buyer', '$2a$10$gcsj7PrZI8xnf31LaUOHXurqBlp.D.UGOICCgpQx7J95Z9C3.aP92', '二手交易买家', 'ROLE_USER')
ON DUPLICATE KEY UPDATE `username`=VALUES(`username`), `password`=VALUES(`password`), `nickname`=VALUES(`nickname`), `role`=VALUES(`role`);

-- 插入初始商品数据
INSERT INTO `goods` (`id`, `name`, `description`, `price`, `image_url`, `status`, `seller_id`, `category`, `condition`, `trading_method`, `location`, `view_count`) VALUES
(1, 'iPhone 13 128G 黑色 国行', '国行无锁，95新，电池健康度88%，无拆修，附赠充电器和保护壳。', 2800.00, 'https://images.unsplash.com/photo-1510557880182-3d4d3cba35a5?w=500&auto=format&fit=crop&q=60', 0, 2, '手机数码', '九成新', '面交/邮寄', '北京市海淀区', 128),
(2, 'iPad Air 5 256G WiFi版 灰色', '几乎全新，今年2月购买，配有原装盒子 and 充电线，送磁吸双面夹。', 3600.00, 'https://images.unsplash.com/photo-1544244015-0df4b3ffc6b0?w=500&auto=format&fit=crop&q=60', 0, 2, '手机数码', '九五新', '面交/邮寄', '北京市朝阳区', 89),
(3, '考研政治/英语官方大纲解析', '正版官方大纲，有少量铅笔划线笔记，适合今年备考的同学冲刺使用。', 15.00, 'https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=500&auto=format&fit=crop&q=60', 0, 2, '图书教材', '八成新', '面交', '北京大学', 256),
(4, '冬季保暖防风加厚羽绒服 L码', '穿过两三次，已干洗，非常暖和，适合身高170-175cm的同学。', 199.00, 'https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=500&auto=format&fit=crop&q=60', 0, 2, '服饰鞋帽', '九成新', '面交/邮寄', '北京市西城区', 67),
(5, '兰蔻清滢柔肤水(粉水) 400ml', '免税店购入，全新未拆封，保质期到2028年，出给需要的集美。', 180.00, 'https://images.unsplash.com/photo-1608248597279-f99d160bfcbc?w=500&auto=format&fit=crop&q=60', 0, 2, '美妆个护', '全新', '邮寄', '上海市浦东新区', 145),
(6, '【限时秒杀】AirPods Pro 3代 蓝牙耳机', '官方正品秒杀专区，支持降噪空间音频，超长续航。数量有限，抢完即止！', 99.00, 'https://images.unsplash.com/photo-1588449668365-d15e397f6787?w=500&auto=format&fit=crop&q=60', 0, 2, '手机数码', '全新', '邮寄', '广州市天河区', 512),
(7, '【绝版竞拍】航海王路飞手办 四档大猿王枪', '全球限定版绝版手办，高约30cm，做工精致，全新彩盒包装，起拍价超值。', 50.00, 'https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?w=500&auto=format&fit=crop&q=60', 0, 2, '潮玩手办', '全新', '邮寄', '深圳市南山区', 320)
ON DUPLICATE KEY UPDATE `name`=VALUES(`name`), `description`=VALUES(`description`), `price`=VALUES(`price`), `image_url`=VALUES(`image_url`), `status`=VALUES(`status`), `seller_id`=VALUES(`seller_id`);

-- 插入秒杀商品关联数据 (Goods ID 为 6)
INSERT INTO `seckill_goods` (`id`, `goods_id`, `seckill_price`, `stock`, `start_time`, `end_time`) VALUES
(1, 6, 99.00, 10, NOW(), DATE_ADD(NOW(), INTERVAL 2 DAY))
ON DUPLICATE KEY UPDATE `seckill_price`=VALUES(`seckill_price`), `stock`=VALUES(`stock`), `start_time`=VALUES(`start_time`), `end_time`=VALUES(`end_time`);

-- 插入拍卖商品关联数据 (Goods ID 为 7)
INSERT INTO `auction_goods` (`id`, `goods_id`, `start_price`, `current_price`, `min_increment`, `start_time`, `end_time`, `status`) VALUES
(1, 7, 50.00, 50.00, 10.00, NOW(), DATE_ADD(NOW(), INTERVAL 12 HOUR), 0)
ON DUPLICATE KEY UPDATE `start_price`=VALUES(`start_price`), `current_price`=VALUES(`current_price`), `min_increment`=VALUES(`min_increment`), `start_time`=VALUES(`start_time`), `end_time`=VALUES(`end_time`), `status`=VALUES(`status`);

-- 9. 收藏表
CREATE TABLE IF NOT EXISTS `favorite` (
    `id` BIGINT AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `goods_id` BIGINT NOT NULL COMMENT '商品ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY uk_user_goods (`user_id`, `goods_id`),
    INDEX idx_user_id (`user_id`),
    INDEX idx_goods_id (`goods_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收藏表';

-- 10. 评价表
CREATE TABLE IF NOT EXISTS `review` (
    `id` BIGINT AUTO_INCREMENT COMMENT '主键ID',
    `order_id` BIGINT NOT NULL COMMENT '订单ID',
    `reviewer_id` BIGINT NOT NULL COMMENT '评价人ID',
    `target_user_id` BIGINT NOT NULL COMMENT '被评价人ID',
    `goods_id` BIGINT NOT NULL COMMENT '商品ID',
    `rating` TINYINT NOT NULL COMMENT '评分: 1-5',
    `content` VARCHAR(1000) COMMENT '评价内容',
    `images` VARCHAR(2000) COMMENT '评价图片JSON',
    `type` TINYINT NOT NULL COMMENT '类型: 1-买家评卖家, 2-卖家评买家',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '评价时间',
    PRIMARY KEY (`id`),
    INDEX idx_order_id (`order_id`),
    INDEX idx_reviewer_id (`reviewer_id`),
    INDEX idx_target_user_id (`target_user_id`),
    INDEX idx_goods_id (`goods_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评价表';

-- 11. 聊天消息历史表
CREATE TABLE IF NOT EXISTS `chat_message` (
    `id` BIGINT AUTO_INCREMENT COMMENT '消息ID',
    `sender_id` BIGINT NOT NULL COMMENT '发送者ID',
    `receiver_id` BIGINT NOT NULL COMMENT '接收者ID',
    `goods_id` BIGINT DEFAULT NULL COMMENT '关联商品ID',
    `content` VARCHAR(2000) NOT NULL COMMENT '消息内容',
    `is_read` TINYINT DEFAULT 0 COMMENT '是否已读: 0-未读, 1-已读',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    PRIMARY KEY (`id`),
    INDEX idx_sender_receiver (`sender_id`, `receiver_id`),
    INDEX idx_receiver_read (`receiver_id`, `is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天消息历史表';


