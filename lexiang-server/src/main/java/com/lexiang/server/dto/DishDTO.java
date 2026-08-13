package com.lexiang.server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class DishDTO {

    @NotBlank(message = "菜品名称不能为空")
    private String name;

    @NotNull(message = "分类不能为空")
    private Long categoryId;

    @NotNull(message = "价格不能为空")
    private BigDecimal price;

    private String image;
    private String description;
    private Integer stock;
    private Integer isHot;
    private Integer isToday;
    private Integer status;
}