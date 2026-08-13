package com.lexiang.server.dto;

import lombok.Data;

@Data
public class PayOrderDTO {
    private Long orderId;
    private String payMethod;  // ALIPAY / WECHAT
}
