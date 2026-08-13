-- ============================================================================
-- 字节智能餐饮平台 (ByteDine) · 数据库
-- 说明：包含全部15张业务表，创建完整数据库结构
-- 用法：docker compose up -d 后由MySQL容器自动执行
-- 编码：utf8mb4 字符集
-- ============================================================================

CREATE DATABASE IF NOT EXISTS `lexiang_food`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_general_ci;

USE `lexiang_food`;
SET NAMES utf8mb4;   -- 强制MySQL客户端使用UTF-8传输，防止Docker环境locale导致乱码


-- 第一部分：用户与权限模块（3张表）
-- 1.1 用户表 (user)：C端消费者账号
-- 用户通过手机号注册、登录，浏览菜品、下单、支付
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
    `id`          bigint      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `phone`       varchar(20) NOT NULL COMMENT '手机号（唯一登录标识）',
    `password`    varchar(255)         DEFAULT NULL COMMENT '密码（BCrypt加密）',
    `nickname`    varchar(50)          DEFAULT NULL COMMENT '昵称',
    `avatar`      varchar(500)         DEFAULT NULL COMMENT '头像URL',
    `status`      tinyint     NOT NULL DEFAULT 1 COMMENT '状态: 0禁用 1正常',
    `create_time` datetime             DEFAULT NULL COMMENT '创建时间',
    `update_time` datetime             DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户信息';


-- ---------------------------------------------------------------------------
-- 1.2 商家表 (merchant)：B端商家账号
-- 商家登录后台管理菜品、订单、查看营收统计
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `merchant`;
CREATE TABLE `merchant` (
    `id`              bigint      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`        varchar(50) NOT NULL COMMENT '登录用户名',
    `password`        varchar(255)         DEFAULT NULL COMMENT '密码（BCrypt加密）',
    `store_name`      varchar(100)         DEFAULT NULL COMMENT '店铺名称',
    `phone`           varchar(20)          DEFAULT NULL COMMENT '手机号',
    `avatar`          varchar(500)         DEFAULT NULL COMMENT '头像URL',
    `status`          tinyint     NOT NULL DEFAULT 1 COMMENT '账号状态: 0禁用 1正常',
    `business_status` tinyint     NOT NULL DEFAULT 1 COMMENT '营业状态: 0已打烊 1营业中',
    `create_time`     datetime             DEFAULT NULL COMMENT '创建时间',
    `update_time`     datetime             DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='商家信息';


-- ---------------------------------------------------------------------------
-- 1.3 员工表 (employee)：商家子账号
-- 商家可为后厨/前台等角色创建子账号，共用商家数据
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `employee`;
CREATE TABLE `employee` (
    `id`          bigint      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `merchant_id` bigint      NOT NULL COMMENT '所属商家ID',
    `username`    varchar(50) NOT NULL COMMENT '用户名',
    `password`    varchar(255)         DEFAULT NULL COMMENT '密码（MD5加密）',
    `phone`       varchar(20)          DEFAULT NULL COMMENT '手机号',
    `role`        varchar(20)          DEFAULT 'STAFF' COMMENT '角色: STAFF后厨 CHEF厨师 MANAGER经理',
    `status`      tinyint     NOT NULL DEFAULT 1 COMMENT '状态: 0禁用 1正常',
    `create_time` datetime             DEFAULT NULL COMMENT '创建时间',
    `update_time` datetime             DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_merchant_id` (`merchant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='商家员工（子账号）';


-- 第二部分：菜品与分类模块（4张表）
-- ---------------------------------------------------------------------------
-- 2.1 分类表 (category)：菜品分组，如「招牌套餐」「面食」「饮品」
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `category`;
CREATE TABLE `category` (
    `id`          bigint      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`        varchar(50) NOT NULL COMMENT '分类名称',
    `merchant_id` bigint      NOT NULL COMMENT '所属商家ID',
    `sort_order`  int         NOT NULL DEFAULT 0 COMMENT '排序（越小越靠前）',
    `status`      tinyint     NOT NULL DEFAULT 1 COMMENT '状态: 0禁用 1启用',
    `create_time` datetime             DEFAULT NULL COMMENT '创建时间',
    `update_time` datetime             DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_merchant_id` (`merchant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='菜品分类';


-- ---------------------------------------------------------------------------
-- 2.2 菜品表 (dish)：上架菜品，含价格、库存、标签等营销字段
-- 用户端展示、购物车、下单均依赖此表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `dish`;
CREATE TABLE `dish` (
    `id`             bigint         NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`           varchar(100)   NOT NULL COMMENT '菜品名称',
    `category_id`    bigint         NOT NULL COMMENT '所属分类ID',
    `merchant_id`    bigint                  DEFAULT NULL COMMENT '所属商家ID',
    `price`          decimal(10,2)  NOT NULL COMMENT '当前售价',
    `original_price` decimal(10,2)           DEFAULT NULL COMMENT '原价（划线对比折扣）',
    `image`          varchar(500)            DEFAULT NULL COMMENT '菜品封面图URL',
    `description`    varchar(500)            DEFAULT NULL COMMENT '菜品描述',
    `sales`          int            NOT NULL DEFAULT 0 COMMENT '销量（用于排序和推荐）',
    `stock`          int            NOT NULL DEFAULT 999 COMMENT '库存数量',
    `is_hot`         tinyint        NOT NULL DEFAULT 0 COMMENT '是否热销: 0否 1是',
    `is_today`       tinyint        NOT NULL DEFAULT 0 COMMENT '是否今日推荐: 0否 1是',
    `status`         tinyint        NOT NULL DEFAULT 1 COMMENT '上下架: 0下架 1上架',
    `rating`         decimal(2,1)            DEFAULT 5.0 COMMENT '评分（1-5星）',
    `label`          varchar(50)             DEFAULT NULL COMMENT '标签: 热门/推荐/新品/热卖',
    `taste_tag`      varchar(50)             DEFAULT NULL COMMENT '口味: 香辣/酸甜/清淡/麻辣',
    `scene_tag`      varchar(50)             DEFAULT NULL COMMENT '场景: 午餐/晚餐/夜宵/下午茶',
    `nutrition_tag`  varchar(50)             DEFAULT NULL COMMENT '营养: 减脂/高蛋白/低卡/高热量',
    `create_time`    datetime                DEFAULT NULL COMMENT '创建时间',
    `update_time`    datetime                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_merchant_id` (`merchant_id`),
    KEY `idx_sales` (`sales`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='菜品';


-- ---------------------------------------------------------------------------
-- 2.3 菜品图片表 (dish_image)：菜品多图轮播（一对多），预留扩展
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `dish_image`;
CREATE TABLE `dish_image` (
    `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `dish_id`     bigint       NOT NULL COMMENT '菜品ID',
    `image_url`   varchar(500) NOT NULL COMMENT '图片URL',
    `sort_order`  int          DEFAULT 0 COMMENT '排序',
    `create_time` datetime     DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_dish_id` (`dish_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='菜品多图';


-- ---------------------------------------------------------------------------
-- 2.4 规格分组表 (spec_group)：规格模板，如「辣度」「份量」「温度」
-- 通过 category_spec_template 关联分类，菜品自动继承分类的规格
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `spec_group`;
CREATE TABLE `spec_group` (
    `id`           bigint      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`         varchar(50) NOT NULL COMMENT '分组名: 辣度/份量/温度/甜度 等',
    `is_required`  tinyint     NOT NULL DEFAULT 1 COMMENT '是否必选: 1必选 0可选',
    `max_select`   int         NOT NULL DEFAULT 1 COMMENT '最多可选数量（is_exclusive=0时生效）',
    `is_exclusive` tinyint     NOT NULL DEFAULT 1 COMMENT '互斥规则: 1互斥（同组单选） 0可多选',
    `sort_order`   int         NOT NULL DEFAULT 0 COMMENT '排序',
    `status`       tinyint     NOT NULL DEFAULT 1 COMMENT '状态: 1启用 0禁用',
    `create_time`  datetime             DEFAULT NULL COMMENT '创建时间',
    `update_time`  datetime             DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='规格分组';


-- ---------------------------------------------------------------------------
-- 2.5 规格选项表 (spec_item)：每个分组下的具体选项，如「微辣」「大份」「加冰」
-- price_extra 表示选择此项需加价的金额
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `spec_item`;
CREATE TABLE `spec_item` (
    `id`          bigint         NOT NULL AUTO_INCREMENT COMMENT '主键',
    `group_id`    bigint         NOT NULL COMMENT '所属规格分组ID',
    `name`        varchar(50)    NOT NULL COMMENT '选项名: 微辣/中辣/大份/加蛋 等',
    `price_extra` decimal(10,2)  NOT NULL DEFAULT 0.00 COMMENT '加价金额',
    `sort_order`  int            NOT NULL DEFAULT 0 COMMENT '排序（0为默认选项）',
    `status`      tinyint        NOT NULL DEFAULT 1 COMMENT '状态: 1启用 0禁用',
    `create_time` datetime                DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_group_id` (`group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='规格选项';


-- ---------------------------------------------------------------------------
-- 2.6 菜品-规格关联表 (dish_spec_rel)：单个菜品自定义规格覆盖（超出分类模板的部分）
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `dish_spec_rel`;
CREATE TABLE `dish_spec_rel` (
    `id`         bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `dish_id`    bigint NOT NULL COMMENT '菜品ID',
    `group_id`   bigint NOT NULL COMMENT '规格分组ID',
    `sort_order` int    DEFAULT 0 COMMENT '排序',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_dish_group` (`dish_id`, `group_id`),
    KEY `idx_dish_id` (`dish_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='菜品-规格关联（覆盖分类模板）';


-- ---------------------------------------------------------------------------
-- 2.7 分类规格模板表 (category_spec_template)：分类的默认规格，菜品自动继承
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `category_spec_template`;
CREATE TABLE `category_spec_template` (
    `id`          bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `category_id` bigint NOT NULL COMMENT '分类ID',
    `group_id`    bigint NOT NULL COMMENT '规格分组ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_category_group` (`category_id`, `group_id`),
    KEY `idx_category_id` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='分类默认规格模板';


-- 第三部分：交易模块（4张表）
-- ---------------------------------------------------------------------------
-- 3.1 购物车表 (shopping_cart)：用户选菜暂存，下单时清空
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `shopping_cart`;
CREATE TABLE `shopping_cart` (
    `id`          bigint         NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`     bigint         NOT NULL COMMENT '用户ID',
    `dish_id`     bigint         NOT NULL COMMENT '菜品ID',
    `quantity`    int            NOT NULL DEFAULT 1 COMMENT '数量',
    `price`       decimal(10,2)           DEFAULT NULL COMMENT '加入时单价（含规格加价）',
    `spec_info`   varchar(255)            DEFAULT NULL COMMENT '规格信息JSON',
    `create_time` datetime                DEFAULT NULL COMMENT '创建时间',
    `update_time` datetime                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_dish_spec` (`user_id`, `dish_id`, `spec_info`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='购物车';


-- ---------------------------------------------------------------------------
-- 3.2 收货地址表 (address)：用户配送地址，下单时快照到订单
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `address`;
CREATE TABLE `address` (
    `id`             bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`        bigint       NOT NULL COMMENT '用户ID',
    `contact_name`   varchar(50)  DEFAULT NULL COMMENT '联系人姓名',
    `contact_phone`  varchar(20)  DEFAULT NULL COMMENT '联系电话',
    `address_detail` varchar(255) DEFAULT NULL COMMENT '详细地址',
    `is_default`     tinyint      NOT NULL DEFAULT 0 COMMENT '是否默认: 0否 1是',
    `create_time`    datetime     DEFAULT NULL COMMENT '创建时间',
    `update_time`    datetime     DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='收货地址';


-- ---------------------------------------------------------------------------
-- 3.3 订单表 (orders)：用户下单记录，含收货信息快照
-- 状态流转: 0待支付→1已支付→2配送中→3已完成；任意状态→4已取消
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `orders`;
CREATE TABLE `orders` (
    `id`            bigint         NOT NULL AUTO_INCREMENT COMMENT '主键',
    `order_no`      varchar(32)    NOT NULL COMMENT '订单号（格式: LX202507070001）',
    `user_id`       bigint         NOT NULL COMMENT '用户ID',
    `merchant_id`   bigint                  DEFAULT NULL COMMENT '商家ID',
    `address_id`    bigint                  DEFAULT NULL COMMENT '收货地址ID',
    `consignee`     varchar(50)             DEFAULT NULL COMMENT '收货人姓名（下单时快照）',
    `phone`         varchar(20)             DEFAULT NULL COMMENT '收货人电话（下单时快照）',
    `address`       varchar(255)            DEFAULT NULL COMMENT '收货地址（下单时快照）',
    `total_amount`  decimal(10,2)  NOT NULL COMMENT '订单总金额',
    `status`        tinyint        NOT NULL DEFAULT 0 COMMENT '订单状态: 0待支付 1已支付 2配送中 3已完成 4已取消',
    `pay_method`    varchar(20)             DEFAULT 'SIMULATE' COMMENT '支付方式',
    `pay_time`      datetime                DEFAULT NULL COMMENT '支付时间',
    `finish_time`   datetime                DEFAULT NULL COMMENT '完成时间',
    `cancel_time`   datetime                DEFAULT NULL COMMENT '取消时间',
    `cancel_reason` varchar(255)            DEFAULT NULL COMMENT '取消原因',
    `remark`        varchar(255)            DEFAULT NULL COMMENT '用户备注',
    `create_time`   datetime                DEFAULT NULL COMMENT '下单时间',
    `update_time`   datetime                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='订单';


-- ---------------------------------------------------------------------------
-- 3.4 订单详情表 (order_detail)：订单中的每个菜品明细（快照）
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `order_detail`;
CREATE TABLE `order_detail` (
    `id`          bigint         NOT NULL AUTO_INCREMENT COMMENT '主键',
    `order_id`    bigint         NOT NULL COMMENT '订单ID',
    `dish_id`     bigint         NOT NULL COMMENT '菜品ID',
    `dish_name`   varchar(100)   NOT NULL COMMENT '菜品名称（下单时快照）',
    `dish_image`  varchar(500)            DEFAULT NULL COMMENT '菜品图片（下单时快照）',
    `price`       decimal(10,2)  NOT NULL COMMENT '下单时单价',
    `quantity`    int            NOT NULL COMMENT '购买数量',
    `amount`      decimal(10,2)  NOT NULL COMMENT '小计金额（price × quantity）',
    `spec_info`   varchar(255)            DEFAULT NULL COMMENT '规格信息JSON',
    `create_time` datetime                DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='订单详情';



-- 第四部分：运营模块（2张表）
-- ---------------------------------------------------------------------------
-- 4.1 轮播图表 (banner)：C端首页 banner 轮播图，最多5张
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `banner`;
CREATE TABLE `banner` (
    `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `title`       varchar(100) DEFAULT NULL COMMENT '标题',
    `image_url`   varchar(500) NOT NULL COMMENT '图片URL',
    `link_url`    varchar(500) DEFAULT NULL COMMENT '跳转链接',
    `sort_order`  int          DEFAULT 0 COMMENT '排序',
    `status`      tinyint      NOT NULL DEFAULT 1 COMMENT '状态: 0禁用 1启用',
    `create_time` datetime     DEFAULT NULL COMMENT '创建时间',
    `update_time` datetime     DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='轮播图';


-- ---------------------------------------------------------------------------
-- 4.2 操作审计日志表 (audit_log)：记录敏感操作，用于安全审计和问题排查
-- 每次下单、接单、取消订单等操作由后端 AOP 自动插入
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `audit_log`;
CREATE TABLE `audit_log` (
    `id`            bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `operator_id`   bigint                DEFAULT NULL COMMENT '操作人ID',
    `operator_type` varchar(20)           DEFAULT NULL COMMENT '操作人类型: USER消费者 / ADMIN商家',
    `module`        varchar(50)  NOT NULL COMMENT '操作模块: 订单管理/菜品管理/规格管理',
    `action`        varchar(100) NOT NULL COMMENT '操作行为: 创建订单/接单/拒单/新增菜品',
    `detail`        varchar(500)          DEFAULT NULL COMMENT '操作详情（订单号、菜品名等）',
    `ip`            varchar(50)           DEFAULT NULL COMMENT '操作IP地址',
    `create_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_operator` (`operator_id`, `operator_type`),
    INDEX `idx_module` (`module`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='操作审计日志';

-----------------------------------------共15张表 --------------------------------------
