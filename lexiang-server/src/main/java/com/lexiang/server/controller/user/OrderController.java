package com.lexiang.server.controller.user;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lexiang.common.result.Result;
import com.lexiang.server.aspect.AuditAnno;
import com.lexiang.server.dto.OrderSubmitDTO;
import com.lexiang.server.service.OrderService;
import com.lexiang.server.vo.OrderVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户端 - 订单管理接口
 * /api/user/order
 *
 * 功能闭环：提交订单 → 查看列表/详情 → 取消 → 确认收货
 */
@RestController("userOrderController")
@RequestMapping("/api/user/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * 获取下单幂等 Token
     * GET /api/user/order/token
     * 前端下单前先获取，提交时放在 Header 带回
     */
    @GetMapping("/token")
    public Result<String> getOrderToken() {
        String token = orderService.generateOrderToken();
        return Result.success(token);
    }

    /**
     * 提交订单
     * POST /api/user/order
     * Body: { "addressId": 1, "remark": "少放辣" }
     * 返回：完整订单信息（含订单号、金额、明细快照）
     */
    @AuditAnno(module = "订单管理", action = "创建订单")
    @PostMapping
    public Result<OrderVO> submit(
            @RequestHeader(value = "X-Order-Token", required = false) String orderToken,
            @Valid @RequestBody OrderSubmitDTO dto) {
        OrderVO vo = orderService.submit(dto, orderToken);
        return Result.success(vo);
    }

    /**
     * 查看订单列表（分页 + 可选状态筛选）
     * GET /api/user/order/list?page=1&pageSize=10&status=0
     * status 可选：0待支付 1已支付 2配送中 3已完成 4已取消
     * 不传则查全部
     */
    @GetMapping("/list")
    public Result<Page<OrderVO>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer status) {
        Page<OrderVO> result = orderService.userPage(page, pageSize, status);
        return Result.success(result);
    }

    /**
     * 查看订单详情（含订单明细快照）
     * GET /api/user/order/{id}
     */
    @GetMapping("/{id}")
    public Result<OrderVO> detail(@PathVariable Long id) {
        OrderVO vo = orderService.getOrderDetail(id);
        return Result.success(vo);
    }

    /**
     * 用户取消订单
     * PUT /api/user/order/{id}/cancel
     * Body: { "reason": "不想买了" }
     */
    @AuditAnno(module = "订单管理", action = "用户取消订单")
    @PutMapping("/{id}/cancel")
    public Result<?> cancel(@PathVariable Long id,
                            @RequestBody Map<String, String> body) {
        orderService.cancel(id, body.get("reason"));
        return Result.success();
    }

    /**
     * 用户确认收货
     * PUT /api/user/order/{id}/confirm
     */
    @PutMapping("/{id}/confirm")
    public Result<?> confirm(@PathVariable Long id) {
        orderService.confirm(id);
        return Result.success();
    }
}