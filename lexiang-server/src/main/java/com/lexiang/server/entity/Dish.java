package com.lexiang.server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("dish")
public class Dish {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;           // 菜品名
    private Long categoryId;       // 分类ID
    private Long merchantId;       // 商家ID
    private BigDecimal price;      // 价格
    private String image;          // 封面图URL
    private String description;    // 描述
    private Integer sales;         // 销量
    private Integer stock;         // 库存
    private Integer isHot;         // 0普通 1热门
    private Integer isToday;       // 0普通 1今日推荐
    private Integer status;        // 0下架 1上架
    private BigDecimal originalPrice; // 原价（划线价）
    private BigDecimal rating;       // 评分 1-5
    private String label;            // 标签: 热门/推荐/新品/热卖
    private String tasteTag;         // 口味: 香辣/酸甜/清淡/麻辣
    private String sceneTag;         // 场景: 午餐/晚餐/夜宵/下午茶
    private String nutritionTag;     // 营养: 减脂/高蛋白/低卡/高热量

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}