package com.lexiang.server.service.ai;

/**
 * 用户端 AI 服务接口
 */
public interface UserAiService {

    /** 智能点餐助手：根据自然语言需求推荐菜品 */
    String suggestDish(Long userId, String demand);

    /** 订单智能客服：回答订单相关问题 */
    String orderConsult(Long userId, Long orderId, String question);
}