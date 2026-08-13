package com.lexiang.server.controller.user;

import com.lexiang.common.result.Result;
import com.lexiang.server.dto.ShoppingCartDTO;
import com.lexiang.server.service.ShoppingCartService;
import com.lexiang.server.vo.CartVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户端 - 购物车接口
 * /api/user/cart
 *
 * 所有接口需要携带用户 Token
 */
@RestController
@RequestMapping("api/user/cart")
@RequiredArgsConstructor
public class ShoppingCartController {
    private final ShoppingCartService cartService;

    /**
     * 查看购物车列表
     * GET /api/user/cart/list
     * 返回当前用户的购物车，含菜品名称、单价、小计
     */
    @GetMapping("list")
    public Result<List<CartVO>> list() {
        List<CartVO> result = cartService.list();
        return Result.success(result);
    }

    /**
     * 加入购物车
     * POST /api/user/cart
     * 同一菜品多次加入会叠加数量，不会重复创建记录
     */
    @PostMapping
    public Result<?> add(@Valid @RequestBody ShoppingCartDTO cartDTO) {
        cartService.add(cartDTO);
        return Result.success();
    }

    /**
     * 修改数量
     * PUT /api/user/cart/1?quantity=3
     */
    @PutMapping("/{cartId}")
    public Result<?> updateQuantiy(@PathVariable Long cartId, @RequestParam Integer quantity) {
        cartService.updateQuantity(cartId, quantity);
        return Result.success();
    }

    /**
     * 删除购物车中的单个菜品
     * DELETE /api/user/cart/1
     */
    @DeleteMapping("/{cartId}")
    public Result<?> delete(@PathVariable Long cartId) {
        cartService.delete(cartId);
        return Result.success();
    }

    /**
     * 清空购物车
     * DELETE /api/user/cart/clear
     */
    @DeleteMapping("/clear")
    public Result<?> clear() {
        cartService.clear();
        return Result.success();
    }
}
