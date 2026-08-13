package com.lexiang.server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 轮播图实体类
 * 首页顶部轮播图，后台管理可编辑
 */
@Data
@TableName("banner")
public class Banner {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;        // 标题
    private String imageUrl;     // 图片URL
    private String linkUrl;      // 跳转链接（可选）
    private Integer sortOrder;   // 排序（越小越靠前）
    private Integer status;      // 0禁用 1启用

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}