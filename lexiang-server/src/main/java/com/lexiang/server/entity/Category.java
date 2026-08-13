package com.lexiang.server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("category")
public class Category {
    @TableId(type = IdType.AUTO)
    /**
     * 菜品分类实体类，对应 category 表
     * 商家后台管理菜品分类，用户端按分类浏览菜品
     */
    private Long id;
    private String name;
    /** 所属商家ID */
    private Long merchantId;
    /** 排序，越小越靠前 */
    private  Integer sortOrder;
    /** 状态：1启用 0禁用 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;



}
