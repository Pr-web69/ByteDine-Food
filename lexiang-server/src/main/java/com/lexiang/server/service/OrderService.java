package com.lexiang.server.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lexiang.server.dto.OrderSubmitDTO;
import com.lexiang.server.vo.OrderVO;

public interface OrderService {
    /** 用户下单 (status=0 待支付) */
    OrderVO submit(OrderSubmitDTO dto, String orderToken);

    /**
     * 生成下单幂等 Token（存 Redis，5分钟有效）
     */
    String generateOrderToken();

    /** 用户查看自己的订单列表 */
    Page<OrderVO> userPage(Integer page,Integer pageSize,Integer status);

    /** 用户根据订单ID查询单条完整订单详情 */
    OrderVO getOrderDetail(Long orderId);

    /** 用户取消订单（仅限待支付 status=0） */
    void cancel(Long orderId,String reason);

    /** 用户确认收货 (status=2→3) */
    void confirm(Long orderId);

    /** 商家查看订单列表 */
    Page<OrderVO> merchantPage(Integer page,Integer pageSize,Integer status,String keyword);

    /** 商家查看单个订单详情 */
    OrderVO getMerchantOrderDetail(Long orderId);

    /** 商家拒单（可拒 status=0 待支付 / status=1 待接单） */
    void merchantCancel(Long orderId,String reason);

    /** 商家接单 (status=1→2 待配送) */
    void accept(Long orderId);

    /** 商家完成订单 (status=2→3 已完成) */
    void complete(Long orderId);

    /** 系统超时取消订单（30分钟未支付，定时任务调用） */
    void cancelTimeoutOrders();

}
