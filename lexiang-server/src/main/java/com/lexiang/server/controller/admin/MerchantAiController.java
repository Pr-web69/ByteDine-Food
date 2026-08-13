package com.lexiang.server.controller.admin;

import com.lexiang.common.result.Result;
import com.lexiang.server.service.StatisticsService;
import com.lexiang.server.service.ai.AiBaseService;
import com.lexiang.server.service.ai.MerchantAiService;
import com.lexiang.server.vo.StatisticsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 商家端 - AI 文案生成 + 经营数据查询
 * /api/admin/ai
 */
@RestController("merchantAiController")
@RequestMapping("/api/admin/ai")
@RequiredArgsConstructor
public class MerchantAiController {

    private final MerchantAiService merchantAiService;
    private final AiBaseService aiBaseService;
    private final StatisticsService statisticsService;

    @PostMapping("/dish-desc")
    public Result<String> dishDesc(@RequestBody Map<String, String> body) {
        return Result.success(
                merchantAiService.generateDishDesc(
                        body.get("dishName"),
                        body.get("feature"),
                        body.getOrDefault("style", "简洁")));
    }

    @PostMapping("/promotion")
    public Result<String> promotion(@RequestBody Map<String, String> body) {
        return Result.success(
                merchantAiService.generatePromotionText(body.get("activityInfo")));
    }

    /**
     * 商家 AI 经营数据查询
     * POST /api/admin/ai/query
     * Body: { "question": "今日营收多少？" }
     *
     * 实现思路：
     * 1. 拉取当前经营数据（StatisticsService）
     * 2. 拼接为 AI Prompt 上下文
     * 3. 调用大模型回答
     */
    @PostMapping("/query")
    public Result<String> query(@RequestBody Map<String, String> body) {
        String question = body.get("question");
        if (question == null || question.isBlank()) {
            return Result.success("请输入您的问题");
        }

        // 拉取经营数据作为 AI 上下文
        StatisticsVO stats = statisticsService.getDashboard();
        StringBuilder context = new StringBuilder();
        context.append("以下是当前平台经营数据：\n");
        context.append("今日营业额：").append(stats.getTodayRevenue()).append("元，");
        context.append("今日订单数：").append(stats.getTodayOrders()).append("单。\n");
        context.append("累计总营业额：").append(stats.getTotalRevenue()).append("元，");
        context.append("累计总订单数：").append(stats.getTotalOrders()).append("单。\n");
        context.append("订单状态统计：");
        if (stats.getOrderStatus() != null) {
            for (Map<String, Object> s : stats.getOrderStatus()) {
                context.append(s.get("name")).append(" ").append(s.get("value")).append("单，");
            }
        }
        context.append("\n热销菜品：");
        if (stats.getTopDishes() != null) {
            for (int i = 0; i < Math.min(5, stats.getTopDishes().size()); i++) {
                Map<String, Object> d = stats.getTopDishes().get(i);
                context.append(d.get("name")).append("(销量").append(d.get("sales")).append(")，");
            }
        }
        context.append("\n请根据以上真实数据回答用户问题，回答简短精准，50字以内。");

        String systemPrompt = "你是字节餐饮平台的经营数据助手。只回答与经营数据、菜品、订单相关的问题。禁止编造数据。";
        String reply = aiBaseService.chat(systemPrompt, context + "\n用户问题：" + question);
        return Result.success(reply);
    }
}