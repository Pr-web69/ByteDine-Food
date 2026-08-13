package com.lexiang.server.vo;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class SpecItemVO {
    private Long itemId;
    private String name;
    private BigDecimal priceExtra;
    private Boolean isDefault;
}
