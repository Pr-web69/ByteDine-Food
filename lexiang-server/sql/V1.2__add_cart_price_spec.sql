-- 购物车字段补充：支持规格、存储加入时单价
ALTER TABLE lexiang_food.shopping_cart
    ADD COLUMN price DECIMAL(10, 2) DEFAULT NULL COMMENT '加入时单价（含规格加价）',
    ADD COLUMN spec_info VARCHAR(255) DEFAULT NULL COMMENT '规格信息 JSON';

-- 原唯一索引只按 user_id + dish_id，无法区分不同规格，改为包含 spec_info
ALTER TABLE lexiang_food.shopping_cart
    DROP INDEX uk_user_dish,
    ADD UNIQUE KEY uk_user_dish_spec (user_id, dish_id, spec_info);
