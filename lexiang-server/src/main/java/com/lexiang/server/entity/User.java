package com.lexiang.server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体类，对应数据库 user 表
 * MyBatis-Plus 会根据 @TableName 自动映射表名，
 * 根据 @TableId 识别主键自增策略
 */
@Data
@TableName("user")
public class User {

    /** 主键，数据库自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    private String phone;

    private String password;

    private String nickname;

    /** 头像URL（MinIO存储路径） */
    private String avatar;

    /** 状态：1正常 0禁用 */
    private Integer status;

    /** 创建时间，插入时由 MyMetaObjectHandler 自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间，插入和更新时由 MyMetaObjectHandler 自动填充 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
