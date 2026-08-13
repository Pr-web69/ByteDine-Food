package com.lexiang.server.dto;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class ShoppingCartDTO {
    private Long dishId;
    private Integer quantity;
    private BigDecimal price;
    private String specInfo;
    /** 用户选中的规格选项ID列表（用于后端互斥校验） */
    private List<Long> specItemIds;
}
