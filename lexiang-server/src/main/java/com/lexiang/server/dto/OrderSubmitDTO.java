package com.lexiang.server.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderSubmitDTO {

    @NotNull(message = "请选择收货地址")
    private Long addressId;

    private String remark;  // 备注，选填
}