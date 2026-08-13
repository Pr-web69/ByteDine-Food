-- 商家营业状态字段
ALTER TABLE merchant ADD COLUMN business_status TINYINT NOT NULL DEFAULT 1 COMMENT '营业状态：1营业中 0已打烊';
