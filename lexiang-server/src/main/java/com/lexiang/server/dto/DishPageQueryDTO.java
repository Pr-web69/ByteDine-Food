package com.lexiang.server.dto;

import lombok.Data;

@Data
public class DishPageQueryDTO {

    private Integer page = 1;
    private Integer pageSize = 10;
    private String name;          // 模糊搜索
    private Long categoryId;      // 按分类筛选
    private Integer status;       // 按状态筛选
    private String sortBy;        // 排序字段: sales / price
    private String sortOrder;     // 排序方向: asc / desc
}