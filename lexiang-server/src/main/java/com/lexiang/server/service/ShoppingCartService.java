package com.lexiang.server.service;

import com.lexiang.server.dto.ShoppingCartDTO;
import com.lexiang.server.vo.CartVO;

import java.util.List;

public interface ShoppingCartService {
    /** 查看当前用户购物车 */
    List<CartVO>list();

    /** 添加购物车 */
    void add(ShoppingCartDTO cartDTO);

    /** 修改购物车 */
    void updateQuantity(Long cartId, Integer quantity);

    /** 删除购物车 */
    void delete(Long cartId);

    /** 清空购物车 */
    void clear();
}
