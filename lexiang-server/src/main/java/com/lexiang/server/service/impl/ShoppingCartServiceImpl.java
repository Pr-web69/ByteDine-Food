package com.lexiang.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lexiang.common.exception.BusinessException;
import com.lexiang.server.dto.ShoppingCartDTO;
import com.lexiang.server.entity.Dish;
import com.lexiang.server.entity.ShoppingCart;
import com.lexiang.server.interceptor.JwtInterceptor;
import com.lexiang.server.mapper.DishMapper;
import com.lexiang.server.mapper.ShoppingCartMapper;
import com.lexiang.server.service.ShoppingCartService;
import com.lexiang.server.service.SpecService;
import com.lexiang.server.vo.CartVO;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShoppingCartServiceImpl implements ShoppingCartService {

    private final ShoppingCartMapper cartMapper;
    private final DishMapper dishMapper;
    private final SpecService specService;

    private Long getUserId() {
        Claims claims = JwtInterceptor.USER_HOLDER.get();
        if (claims == null) throw new BusinessException(401, "请先登录");
        return claims.get("userId", Long.class);
    }

    @Override
    public List<CartVO> list() {
        Long userId = getUserId();
        LambdaQueryWrapper<ShoppingCart> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShoppingCart::getUserId, userId).orderByDesc(ShoppingCart::getCreateTime);
        List<ShoppingCart> cartList = cartMapper.selectList(wrapper);

        List<CartVO> voList = new ArrayList<>();
        for (ShoppingCart cart : cartList) {
            Dish dish = dishMapper.selectById(cart.getDishId());
            if (dish == null || Integer.valueOf(0).equals(dish.getStatus())) continue;

            CartVO vo = new CartVO();
            vo.setCartId(cart.getId());
            vo.setDishId(dish.getId());
            vo.setDishName(dish.getName());
            vo.setDishImage(dish.getImage());
            // 优先用购物车快照价格（含规格加价），否则用菜品原价
            BigDecimal price = cart.getPrice() != null ? cart.getPrice() : dish.getPrice();
            vo.setPrice(price);
            vo.setQuantity(cart.getQuantity());
            vo.setAmount(price.multiply(BigDecimal.valueOf(cart.getQuantity())));
            vo.setSpecInfo(cart.getSpecInfo());
            vo.setCreateTime(cart.getCreateTime());
            voList.add(vo);
        }
        return voList;
    }

    @Override
    public void add(ShoppingCartDTO cartDTO) {
        Long userId = getUserId();
        Long dishId = cartDTO.getDishId();

        Dish dish = dishMapper.selectById(dishId);
        if (dish == null || !Integer.valueOf(1).equals(dish.getStatus())) {
            throw new BusinessException(400, "菜品已下架或不存在");
        }

        // 规格互斥校验：前端传了 specItemIds 时校验
        if (cartDTO.getSpecItemIds() != null && !cartDTO.getSpecItemIds().isEmpty()) {
            specService.validateExclusive(dishId, cartDTO.getSpecItemIds());
        }

        LambdaQueryWrapper<ShoppingCart> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShoppingCart::getUserId, userId).eq(ShoppingCart::getDishId, dishId);

        ShoppingCart exist = cartMapper.selectOne(wrapper);
        // 当规格不同时视为不同的购物车条目
        String newSpec = cartDTO.getSpecInfo();
        if (exist != null && java.util.Objects.equals(exist.getSpecInfo(), newSpec)) {
            exist.setQuantity(exist.getQuantity() + (cartDTO.getQuantity() != null ? cartDTO.getQuantity() : 1));
            cartMapper.updateById(exist);
            return;
        }

        ShoppingCart cart = new ShoppingCart();
        cart.setUserId(userId);
        cart.setDishId(dishId);
        cart.setQuantity(cartDTO.getQuantity() != null ? cartDTO.getQuantity() : 1);
        // 存储规格信息和含加价的实际单价，前端未传时回退到菜品原价
        cart.setSpecInfo(newSpec);
        BigDecimal price = cartDTO.getPrice() != null ? cartDTO.getPrice() : dish.getPrice();
        cart.setPrice(price);
        cartMapper.insert(cart);
    }

    @Override
    public void updateQuantity(Long cartId, Integer quantity) {
        ShoppingCart cart = cartMapper.selectById(cartId);
        if (cart == null) throw new BusinessException(400, "购物车记录不存在");
        if (!cart.getUserId().equals(getUserId())) throw new BusinessException(403, "无权限操作");
        cart.setQuantity(quantity);
        cartMapper.updateById(cart);
    }

    @Override
    public void delete(Long cartId) {
        ShoppingCart cart = cartMapper.selectById(cartId);
        if (cart == null) throw new BusinessException(400, "购物车记录不存在");
        if (!cart.getUserId().equals(getUserId())) throw new BusinessException(403, "无权限操作");
        cartMapper.deleteById(cartId);
    }

    @Override
    public void clear() {
        Long userId = getUserId();
        LambdaQueryWrapper<ShoppingCart> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShoppingCart::getUserId, userId);
        cartMapper.delete(wrapper);
    }
}
