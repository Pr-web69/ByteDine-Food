package com.lexiang.server.vo;

import com.lexiang.server.entity.OrderDetail;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderVO {

    private Long id;
    private String orderNo;
    private BigDecimal totalAmount;
    private Integer status;
    private String statusText;     // 状态文字：待支付/已支付/配送中...
    private String payMethod;
    private String remark;
    private String cancelReason;
    private LocalDateTime createTime;
    private LocalDateTime payTime;
    private LocalDateTime cancelTime;
    private LocalDateTime finishTime;
    // 收货信息（下单时快照，用户端脱敏）
    private String consignee;      // 收货人
    private String phone;
    private String address;

    private List<OrderDetail> details;  // 订单菜品详情
}