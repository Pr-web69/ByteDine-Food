package com.lexiang.server.service.ai;

/**
 * 商家端 AI 服务接口
 */
public interface MerchantAiService {

    /** 菜品文案生成 */
    String generateDishDesc(String dishName, String feature, String style);

    /** 营销活动文案生成 */
    String generatePromotionText(String activityInfo);
}