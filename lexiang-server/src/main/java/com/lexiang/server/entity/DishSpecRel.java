package com.lexiang.server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("dish_spec_rel")
public class DishSpecRel {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long dishId;
    private Long groupId;
    private Integer sortOrder;
}
