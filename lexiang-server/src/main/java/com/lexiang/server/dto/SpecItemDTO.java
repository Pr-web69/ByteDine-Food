package com.lexiang.server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class SpecItemDTO {
    @NotNull(message = "分组ID不能为空")
    private Long groupId;
    @NotBlank(message = "选项名称不能为空")
    private String name;
    private BigDecimal priceExtra;
    private Integer sortOrder;
    private Integer status;
}
