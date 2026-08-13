package com.lexiang.server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商家实体类，对应数据库 merchant 表
 * 商家登录后可在后台管理菜品、订单、数据统计
 */
@Data
@TableName("merchant")
public class Merchant {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String password;

    private String storeName;

    private String phone;

    /** 头像URL（MinIO存储路径） */
    private String avatar;

    /** 状态：1正常 0禁用 */
    private Integer status;

    /** 营业状态：1营业中 0已打烊 */
    private Integer businessStatus;

    /** 创建时间，插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间，插入和更新时自动填充 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
