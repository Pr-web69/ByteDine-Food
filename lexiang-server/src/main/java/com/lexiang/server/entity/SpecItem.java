package com.lexiang.server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@TableName("spec_item")
public class SpecItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long groupId;
    private String name;
    private BigDecimal priceExtra;
    private Integer sortOrder;
    private Integer status;
}
