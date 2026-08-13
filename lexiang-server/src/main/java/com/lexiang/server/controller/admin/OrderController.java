package com.lexiang.server.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lexiang.common.result.Result;
import com.lexiang.server.aspect.AuditAnno;
import com.lexiang.server.service.OrderService;
import com.lexiang.server.vo.OrderVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 商家端 - 订单管理接口
 * /api/admin/order
 * 状态流转：0待支付 → 1待接单(支付后) → 2待配送(接单) → 3已完成
 * 商家操作：拒单(0/1→4)、接单(1→2)、完成(2→3)
 */
@RestController("adminOrderController")
@RequestMapping("/api/admin/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * 商家查看本店订单列表（分页 + 状态筛选 + 订单号搜索）
     * GET /api/admin/order/list?page=1&pageSize=10&status=0&keyword=LX20260716
     */
    @GetMapping("/list")
    public Result<Page<OrderVO>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {
        Page<OrderVO> result = orderService.merchantPage(page, pageSize, status, keyword);
        return Result.success(result);
    }

    /**
     * 商家查看单个订单详情
     * GET /api/admin/order/{id}
     */
    @GetMapping("/{id}")
    public Result<OrderVO> detail(@PathVariable Long id) {
        return Result.success(orderService.getMerchantOrderDetail(id));
    }

    /**
     * 商家拒单（待支付/待接单均可拒）
     * PUT /api/admin/order/{id}/cancel
     * Body: { "reason": "缺货" }
     */
    @AuditAnno(module = "订单管理", action = "商家拒单")
    @PutMapping("/{id}/cancel")
    public Result<?> cancel(@PathVariable Long id,
                            @RequestBody Map<String, String> body) {
        orderService.merchantCancel(id, body.get("reason"));
        return Result.success();
    }

    /**
     * 商家接单 (status 1→2 待配送)
     * PUT /api/admin/order/{id}/accept
     */
    @AuditAnno(module = "订单管理", action = "商家接单")
    @PutMapping("/{id}/accept")
    public Result<?> accept(@PathVariable Long id) {
        orderService.accept(id);
        return Result.success();
    }

    /**
     * 商家完成订单 (status 2→3 已完成)
     * PUT /api/admin/order/{id}/complete
     */
    @AuditAnno(module = "订单管理", action = "商家完成订单")
    @PutMapping("/{id}/complete")
    public Result<?> complete(@PathVariable Long id) {
        orderService.complete(id);
        return Result.success();
    }
}