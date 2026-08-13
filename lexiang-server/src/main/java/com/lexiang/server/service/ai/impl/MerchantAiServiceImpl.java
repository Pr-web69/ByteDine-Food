package com.lexiang.server.service.ai.impl;

import com.lexiang.server.constant.AiPromptTemplate;
import com.lexiang.server.service.ai.AiBaseService;
import com.lexiang.server.service.ai.MerchantAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 商家端 AI 服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantAiServiceImpl implements MerchantAiService {

    private final AiBaseService aiBaseService;

    /**
     * 菜品文案生成
     * @param dishName 菜品名
     * @param feature  食材/口味特点
     * @param style    风格：简洁/网红/正式
     */
    @Override
    public String generateDishDesc(String dishName, String feature, String style) {
        String prompt = String.format("""
                菜品名：%s
                食材特点：%s
                风格要求：%s
                请生成一段 150 字以内的菜品宣传文案。
                """, dishName, feature, style);

        return aiBaseService.chat(AiPromptTemplate.DISH_COPY_SYSTEM, prompt);
    }

    /**
     * 营销活动文案生成
     */
    @Override
    public String generatePromotionText(String activityInfo) {
        String prompt = String.format("""
                请为以下活动撰写一段 200 字以内的营销宣传文案，适用于校园外卖平台：
                %s
                """, activityInfo);

        return aiBaseService.chat(AiPromptTemplate.PROMOTION_SYSTEM, prompt);
    }
}