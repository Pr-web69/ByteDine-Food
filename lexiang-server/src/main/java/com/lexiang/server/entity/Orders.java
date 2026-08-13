package com.lexiang.server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data
@TableName("orders")
public class Orders {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;        // 订单号 LX20260716001
    private Long userId;           // 下单用户
    private Long merchantId;       // 商家ID
    private Long addressId;        // 收货地址ID
    private String consignee;      // 收货人姓名（下单时快照）
    private String phone;          // 收货人手机号（下单时快照）
    private String address;        // 收货地址（下单时快照）
    private BigDecimal totalAmount;// 总金额 BigDecimal高精度浮点运算类
    private Integer status;        // 0待支付 1已支付 2配送中 3已完成 4已取消
    private String payMethod;      // 支付方式 SIMULATE
    private LocalDateTime payTime;
    private LocalDateTime finishTime;
    private LocalDateTime cancelTime;
    private String cancelReason;
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

}
