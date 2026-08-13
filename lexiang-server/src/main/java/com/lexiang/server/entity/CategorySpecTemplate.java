package com.lexiang.server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("category_spec_template")
public class CategorySpecTemplate {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long categoryId;
    private Long groupId;
}
