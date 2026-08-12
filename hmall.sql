-- =============================================
-- 黑马商城（hmall）数据库初始化脚本
-- =============================================

-- 创建数据库
DROP DATABASE IF EXISTS `hmall`;
CREATE DATABASE `hmall` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `hmall`;

-- =============================================
-- 1. 用户表（user）
-- =============================================
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '用户id',
  `username` VARCHAR(100) DEFAULT NULL COMMENT '用户名',
  `password` VARCHAR(128) NOT NULL COMMENT '密码，加密存储',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '注册手机号',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `status` TINYINT(1) DEFAULT 1 COMMENT '使用状态（1正常 2冻结）',
  `balance` INT(11) DEFAULT 0 COMMENT '账户余额（单位：分）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- =============================================
-- 2. 商品表（item）
-- =============================================
DROP TABLE IF EXISTS `item`;
CREATE TABLE `item` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '商品id',
  `name` VARCHAR(200) NOT NULL COMMENT 'SKU名称',
  `price` INT(11) NOT NULL COMMENT '价格（单位：分）',
  `stock` INT(11) NOT NULL DEFAULT 0 COMMENT '库存数量',
  `image` VARCHAR(500) DEFAULT NULL COMMENT '商品图片',
  `category` VARCHAR(50) DEFAULT NULL COMMENT '类目名称',
  `brand` VARCHAR(50) DEFAULT NULL COMMENT '品牌名称',
  `spec` VARCHAR(200) DEFAULT NULL COMMENT '规格',
  `sold` INT(11) DEFAULT 0 COMMENT '销量',
  `comment_count` INT(11) DEFAULT 0 COMMENT '评论数',
  `isAD` TINYINT(1) DEFAULT 0 COMMENT '是否是推广广告，0-否，1-是',
  `status` TINYINT(1) DEFAULT 1 COMMENT '商品状态 1-正常，2-下架，3-删除',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creater` BIGINT(20) DEFAULT NULL COMMENT '创建人',
  `updater` BIGINT(20) DEFAULT NULL COMMENT '修改人',
  PRIMARY KEY (`id`),
  KEY `idx_name` (`name`),
  KEY `idx_category` (`category`),
  KEY `idx_brand` (`brand`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- =============================================
-- 3. 订单表（order）
-- =============================================
DROP TABLE IF EXISTS `order`;
CREATE TABLE `order` (
  `id` BIGINT(20) NOT NULL COMMENT '订单id',
  `total_fee` INT(11) NOT NULL COMMENT '总金额，单位为分',
  `payment_type` TINYINT(1) DEFAULT 1 COMMENT '支付类型，1、支付宝，2、微信，3、扣减余额',
  `user_id` BIGINT(20) NOT NULL COMMENT '用户id',
  `status` TINYINT(1) DEFAULT 1 COMMENT '订单的状态，1、未付款 2、已付款,未发货 3、已发货,未确认 4、确认收货，交易成功 5、交易取消，订单关闭 6、交易结束，已评价',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `pay_time` DATETIME DEFAULT NULL COMMENT '支付时间',
  `consign_time` DATETIME DEFAULT NULL COMMENT '发货时间',
  `end_time` DATETIME DEFAULT NULL COMMENT '交易完成时间',
  `close_time` DATETIME DEFAULT NULL COMMENT '交易关闭时间',
  `comment_time` DATETIME DEFAULT NULL COMMENT '评价时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- =============================================
-- 4. 订单详情表（order_detail）
-- =============================================
DROP TABLE IF EXISTS `order_detail`;
CREATE TABLE `order_detail` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '订单详情id',
  `order_id` BIGINT(20) NOT NULL COMMENT '订单id',
  `item_id` BIGINT(20) NOT NULL COMMENT '商品id',
  `num` INT(11) NOT NULL COMMENT '商品购买数量',
  `name` VARCHAR(200) NOT NULL COMMENT '商品标题',
  `spec` VARCHAR(200) DEFAULT NULL COMMENT '商品规格',
  `price` INT(11) NOT NULL COMMENT '商品价格（单位：分）',
  `image` VARCHAR(500) DEFAULT NULL COMMENT '商品图片',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_item_id` (`item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单详情表';

-- =============================================
-- 5. 购物车表（cart）
-- =============================================
DROP TABLE IF EXISTS `cart`;
CREATE TABLE `cart` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '购物车id',
  `user_id` BIGINT(20) NOT NULL COMMENT '用户id',
  `item_id` BIGINT(20) NOT NULL COMMENT '商品id',
  `num` INT(11) NOT NULL DEFAULT 1 COMMENT '购买数量',
  `name` VARCHAR(200) NOT NULL COMMENT '商品标题',
  `spec` VARCHAR(200) DEFAULT NULL COMMENT '商品规格',
  `price` INT(11) NOT NULL COMMENT '商品价格（单位：分）',
  `image` VARCHAR(500) DEFAULT NULL COMMENT '商品图片',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_item_id` (`item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购物车表';

-- =============================================
-- 6. 地址表（address）
-- =============================================
DROP TABLE IF EXISTS `address`;
CREATE TABLE `address` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '地址id',
  `user_id` BIGINT(20) NOT NULL COMMENT '用户id',
  `province` VARCHAR(30) DEFAULT NULL COMMENT '省',
  `city` VARCHAR(30) DEFAULT NULL COMMENT '市',
  `town` VARCHAR(30) DEFAULT NULL COMMENT '区/县',
  `mobile` VARCHAR(20) NOT NULL COMMENT '手机号',
  `street` VARCHAR(200) DEFAULT NULL COMMENT '详细地址',
  `contact` VARCHAR(50) DEFAULT NULL COMMENT '联系人',
  `is_default` TINYINT(1) DEFAULT 0 COMMENT '是否是默认地址，0-否，1-是',
  `notes` VARCHAR(200) DEFAULT NULL COMMENT '备注',
  `deleted` TINYINT(1) DEFAULT 0 COMMENT '是否删除，0-否，1-是',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='地址表';

-- =============================================
-- 7. 订单物流表（order_logistics）
-- =============================================
DROP TABLE IF EXISTS `order_logistics`;
CREATE TABLE `order_logistics` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '物流id',
  `order_id` BIGINT(20) NOT NULL COMMENT '订单id',
  `logistics_number` VARCHAR(50) DEFAULT NULL COMMENT '物流单号',
  `logistics_company` VARCHAR(50) DEFAULT NULL COMMENT '物流公司',
  `contact` VARCHAR(50) DEFAULT NULL COMMENT '联系人',
  `mobile` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  `province` VARCHAR(30) DEFAULT NULL COMMENT '省',
  `city` VARCHAR(30) DEFAULT NULL COMMENT '市',
  `town` VARCHAR(30) DEFAULT NULL COMMENT '区/县',
  `street` VARCHAR(200) DEFAULT NULL COMMENT '详细地址',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单物流表';

-- =============================================
-- 8. 支付订单表（pay_order）
-- =============================================
DROP TABLE IF EXISTS `pay_order`;
CREATE TABLE `pay_order` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '支付订单id',
  `biz_order_no` BIGINT(20) NOT NULL COMMENT '业务订单号',
  `pay_order_no` BIGINT(20) NOT NULL COMMENT '支付单号',
  `biz_user_id` BIGINT(20) NOT NULL COMMENT '业务用户id',
  `payment_channel` VARCHAR(20) DEFAULT NULL COMMENT '支付渠道',
  `amount` INT(11) NOT NULL COMMENT '支付金额（单位：分）',
  `pay_type` TINYINT(1) DEFAULT 1 COMMENT '支付类型，1、h5,2、小程序，3、公众号，4、扫码',
  `status` TINYINT(1) DEFAULT 1 COMMENT '支付状态，0、未支付，1、已支付，2、已关闭',
  `pay_success_time` DATETIME DEFAULT NULL COMMENT '支付成功时间',
  `pay_order_expire_time` DATETIME DEFAULT NULL COMMENT '支付订单过期时间',
  `pay_channel_extra` VARCHAR(500) DEFAULT NULL COMMENT '支付渠道额外参数',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_biz_order_no` (`biz_order_no`),
  UNIQUE KEY `uk_pay_order_no` (`pay_order_no`),
  KEY `idx_biz_user_id` (`biz_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付订单表';

-- =============================================
-- 插入测试数据
-- =============================================

-- 插入测试用户（密码：123456，使用 BCrypt 加密）
INSERT INTO `user` VALUES 
(1, 'admin', '$2a$10$pGXGsLU2KqgEBMGN6d3qvO2jFCu0R1zKDUvCakyQKGZS96tWyR6gW', '13800138000', NOW(), NOW(), 1, 100000),
(2, 'user1', '$2a$10$pGXGsLU2KqgEBMGN6d3qvO2jFCu0R1zKDUvCakyQKGZS96tWyR6gW', '13800138001', NOW(), NOW(), 1, 50000),
(3, 'user2', '$2a$10$pGXGsLU2KqgEBMGN6d3qvO2jFCu0R1zKDUvCakyQKGZS96tWyR6gW', '13800138002', NOW(), NOW(), 1, 30000);

-- 插入测试商品
INSERT INTO `item` VALUES 
(1, 'iPhone 14 Pro 256GB 深空黑', 799900, 100, 'https://example.com/iphone14.jpg', '手机', '苹果', '256GB 深空黑', 520, 128, 1, 1, NOW(), NOW(), 1, 1),
(2, '华为 Mate 50 Pro 256GB 昆仑破晓', 689900, 200, 'https://example.com/mate50.jpg', '手机', '华为', '256GB 昆仑破晓', 380, 95, 0, 1, NOW(), NOW(), 1, 1),
(3, '小米 13 Pro 256GB 陶瓷白', 499900, 150, 'https://example.com/mi13.jpg', '手机', '小米', '256GB 陶瓷白', 680, 210, 0, 1, NOW(), NOW(), 1, 1),
(4, 'MacBook Pro 14英寸 M2 芯片', 1499900, 50, 'https://example.com/macbook.jpg', '笔记本', '苹果', 'M2 芯片 16GB 512GB', 150, 45, 1, 1, NOW(), NOW(), 1, 1),
(5, 'AirPods Pro 2代', 189900, 300, 'https://example.com/airpods.jpg', '耳机', '苹果', '主动降噪', 1200, 380, 0, 1, NOW(), NOW(), 1, 1);

-- =============================================
-- 完成
-- =============================================
SELECT '数据库初始化完成！' AS message;
