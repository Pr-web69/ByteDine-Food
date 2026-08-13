package com.lexiang.server.service.ai.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lexiang.server.constant.AiPromptTemplate;
import com.lexiang.server.entity.Dish;
import com.lexiang.server.entity.OrderDetail;
import com.lexiang.server.entity.Orders;
import com.lexiang.server.mapper.DishMapper;
import com.lexiang.server.mapper.OrderDetailMapper;
import com.lexiang.server.mapper.OrdersMapper;
import com.lexiang.server.service.ai.AiBaseService;
import com.lexiang.server.service.ai.UserAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户端 AI 服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserAiServiceImpl implements UserAiService {

    private final AiBaseService aiBaseService;
    private final DishMapper dishMapper;
    private final OrdersMapper ordersMapper;
    private final OrderDetailMapper orderDetailMapper;

    /**
     * 智能点餐助手
     * 基于真实菜品数据 + 用户历史订单偏好，AI 匹配推荐
     */
    @Override
    public String suggestDish(Long userId, String demand) {
        // 1. 查在售菜品
        LambdaQueryWrapper<Dish> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Dish::getStatus, 1);
        List<Dish> dishes = dishMapper.selectList(wrapper);
        if (dishes.isEmpty()) {
            return "当前暂无菜品在售，请稍后刷新。";
        }

        String dishList = dishes.stream()
                .map(d -> String.format("[ID:%d] %s ￥%.2f - %s",
                        d.getId(), d.getName(), d.getPrice(),
                        d.getDescription() != null ? d.getDescription() : "暂无描述"))
                .collect(Collectors.joining("\n"));

        // 2. 查用户历史订单（最近 3 条已完成订单的菜品名）
        String historyInfo = "无历史订单";
        if (userId != null) {
            LambdaQueryWrapper<Orders> orderWrapper = new LambdaQueryWrapper<>();
            orderWrapper.eq(Orders::getUserId, userId)
                    .eq(Orders::getStatus, 3)
                    .orderByDesc(Orders::getCreateTime)
                    .last("LIMIT 3");
            List<Orders> historyOrders = ordersMapper.selectList(orderWrapper);
            if (!historyOrders.isEmpty()) {
                List<String> historyNames = new java.util.ArrayList<>();
                for (Orders o : historyOrders) {
                    LambdaQueryWrapper<OrderDetail> dWrapper = new LambdaQueryWrapper<>();
                    dWrapper.eq(OrderDetail::getOrderId, o.getId());
                    List<OrderDetail> details = orderDetailMapper.selectList(dWrapper);
                    for (OrderDetail d : details) {
                        historyNames.add(d.getDishName());
                    }
                }
                historyInfo = String.join("、", historyNames.subList(0,
                        Math.min(3, historyNames.size())));
            }
        }

        // 3. 组装 Prompt 调 AI
        String userPrompt = String.format(AiPromptTemplate.ORDER_ASSISTANT_USER,
                dishList, demand, historyInfo);

        return aiBaseService.chat(AiPromptTemplate.ORDER_ASSISTANT_SYSTEM, userPrompt);
    }

    /**
     * 订单智能客服
     * 传入订单信息作为上下文，让 AI 基于真实数据回答
     */
    @Override
    public String orderConsult(Long userId, Long orderId, String question) {
        // 查订单
        Orders order = ordersMapper.selectById(orderId);
        if (order == null) {
            return "订单不存在，请确认订单号是否正确。";
        }
        if (!order.getUserId().equals(userId)) {
            return "无权限查看该订单。";
        }

        // 查订单明细
        LambdaQueryWrapper<OrderDetail> detailWrapper = new LambdaQueryWrapper<>();
        detailWrapper.eq(OrderDetail::getOrderId, orderId);
        List<OrderDetail> details = orderDetailMapper.selectList(detailWrapper);

        String statusText = switch (order.getStatus()) {
            case 0 -> "待支付";
            case 1 -> "已支付";
            case 2 -> "配送中";
            case 3 -> "已完成";
            case 4 -> "已取消";
            default -> "未知";
        };

        String items = details.stream()
                .map(d -> d.getDishName() + " x" + d.getQuantity())
                .collect(Collectors.joining("、"));

        String context = String.format("""
                订单号：%s
                状态：%s
                商品：%s
                金额：￥%.2f
                下单时间：%s
                """, order.getOrderNo(), statusText, items,
                order.getTotalAmount(), order.getCreateTime());

        String userPrompt = "请根据以下订单信息回答用户问题。\n" + context + "\n用户问题：" + question;

        return aiBaseService.chat(AiPromptTemplate.ORDER_CONSULT_SYSTEM, userPrompt);
    }
}