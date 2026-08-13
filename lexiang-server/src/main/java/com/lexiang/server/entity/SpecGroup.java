package com.lexiang.server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("spec_group")
public class SpecGroup {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private Integer isRequired;
    private Integer maxSelect;
    private Integer isExclusive;
    private Integer sortOrder;
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
