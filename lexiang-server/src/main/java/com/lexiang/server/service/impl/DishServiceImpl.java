package com.lexiang.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lexiang.common.constant.RedisConstants;
import com.lexiang.common.exception.BusinessException;
import com.lexiang.server.dto.DishDTO;
import com.lexiang.server.dto.DishPageQueryDTO;
import com.lexiang.server.entity.Dish;
import com.lexiang.server.interceptor.JwtInterceptor;
import com.lexiang.server.mapper.DishMapper;
import com.lexiang.server.service.DishService;
import com.lexiang.server.service.SpecService;
import com.lexiang.server.vo.DishWithSpecsVO;
import com.lexiang.server.vo.SpecGroupVO;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DishServiceImpl implements DishService {

    private final DishMapper dishMapper;
    private final SpecService specService;
    private final RedisTemplate<String, Object> redisTemplate;

    private Long getMerchantId() {
        Claims claims = JwtInterceptor.USER_HOLDER.get();
        if (claims == null) throw new BusinessException(401, "请先登录");
        return claims.get("userId", Long.class);
    }

    @Override
    public Page<Dish> pageQuery(DishPageQueryDTO queryDTO) {
        Page<Dish> pageInfo = new Page<>(queryDTO.getPage(), queryDTO.getPageSize());
        LambdaQueryWrapper<Dish> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Dish::getMerchantId, getMerchantId());
        wrapper.like(StringUtils.hasText(queryDTO.getName()), Dish::getName, queryDTO.getName());
        wrapper.eq(queryDTO.getCategoryId() != null, Dish::getCategoryId, queryDTO.getCategoryId());
        wrapper.eq(queryDTO.getStatus() != null, Dish::getStatus, queryDTO.getStatus());
        if ("sales".equals(queryDTO.getSortBy())) {
            wrapper.orderBy(true, "asc".equals(queryDTO.getSortOrder()), Dish::getSales);
        } else if ("price".equals(queryDTO.getSortBy())) {
            wrapper.orderBy(true, "asc".equals(queryDTO.getSortOrder()), Dish::getPrice);
        } else {
            wrapper.orderByDesc(Dish::getCreateTime);
        }
        dishMapper.selectPage(pageInfo, wrapper);
        return pageInfo;
    }

    /** 安全删除 Redis 缓存（Redis 不可用时不影响业务） */
    private void safeDeleteCache(String key) {
        try { redisTemplate.delete(key); } catch (Exception e) { log.warn("Redis delete failed for {}: {}", key, e.getMessage()); }
    }

    @Override
    public void add(DishDTO dishDTO) {
        Dish dish = new Dish();
        copyProperties(dishDTO, dish);
        dish.setMerchantId(getMerchantId());
        dish.setSales(0);
        if (dish.getStock() == null) dish.setStock(999);
        if (dish.getStatus() == null) dish.setStatus(1);
        if (dish.getIsHot() == null) dish.setIsHot(0);
        if (dish.getIsToday() == null) dish.setIsToday(0);
        dishMapper.insert(dish);
        safeDeleteCache(RedisConstants.DISH_HOT);
        safeDeleteCache(RedisConstants.DISH_TODAY);
    }

    @Override
    public void update(Long id, DishDTO dishDTO) {
        Dish dish = dishMapper.selectById(id);
        if (dish == null) throw new BusinessException(400, "菜品不存在");
        if (!dish.getMerchantId().equals(getMerchantId())) throw new BusinessException(403, "无权限操作该菜品");
        copyProperties(dishDTO, dish);
        dishMapper.updateById(dish);
        safeDeleteCache(RedisConstants.DISH_HOT);
        safeDeleteCache(RedisConstants.DISH_TODAY);
    }

    @Override
    public void delete(Long id) {
        Dish dish = dishMapper.selectById(id);
        if (dish == null) throw new BusinessException(400, "菜品不存在");
        if (!dish.getMerchantId().equals(getMerchantId())) throw new BusinessException(403, "无权限操作该菜品");
        dishMapper.deleteById(id);
        safeDeleteCache(RedisConstants.DISH_HOT);
        safeDeleteCache(RedisConstants.DISH_TODAY);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        Dish dish = dishMapper.selectById(id);
        if (dish == null) throw new BusinessException(400, "菜品不存在");
        if (!dish.getMerchantId().equals(getMerchantId())) throw new BusinessException(403, "无权限操作该菜品");
        dish.setStatus(status);
        dishMapper.updateById(dish);
        safeDeleteCache(RedisConstants.DISH_HOT);
        safeDeleteCache(RedisConstants.DISH_TODAY);
    }

    private void copyProperties(DishDTO dto, Dish dish) {
        dish.setName(dto.getName());
        dish.setCategoryId(dto.getCategoryId());
        dish.setPrice(dto.getPrice());
        dish.setImage(dto.getImage());
        dish.setDescription(dto.getDescription());
        dish.setStock(dto.getStock());
        dish.setIsHot(dto.getIsHot());
        dish.setIsToday(dto.getIsToday());
        dish.setStatus(dto.getStatus());
    }

    private List<DishWithSpecsVO> attachSpecs(List<Dish> dishes) {
        if (dishes == null || dishes.isEmpty()) return Collections.emptyList();
        Map<Long, Long> dishCategoryMap = dishes.stream()
                .collect(Collectors.toMap(Dish::getId, d -> d.getCategoryId() != null ? d.getCategoryId() : 0L,
                        (a, b) -> a, LinkedHashMap::new));
        Map<Long, List<SpecGroupVO>> specsMap = specService.getSpecsBatch(dishCategoryMap);
        return dishes.stream().map(dish -> {
            DishWithSpecsVO vo = new DishWithSpecsVO();
            BeanUtils.copyProperties(dish, vo);
            List<SpecGroupVO> specGroups = specsMap.getOrDefault(dish.getId(), Collections.emptyList());
            vo.setHasSpec(!specGroups.isEmpty());
            vo.setSpecGroups(specGroups);
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<DishWithSpecsVO> getHotDishes() {
        List<Dish> cached = (List<Dish>) redisTemplate.opsForValue().get(RedisConstants.DISH_HOT);
        if (cached != null) return attachSpecs(cached);
        LambdaQueryWrapper<Dish> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Dish::getIsHot, 1).eq(Dish::getStatus, 1).orderByDesc(Dish::getSales);
        List<Dish> list = dishMapper.selectList(wrapper);
        redisTemplate.opsForValue().set(RedisConstants.DISH_HOT, list, RedisConstants.CACHE_TTL, TimeUnit.MINUTES);
        return attachSpecs(list);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<DishWithSpecsVO> getTodayDishes() {
        List<Dish> cached = (List<Dish>) redisTemplate.opsForValue().get(RedisConstants.DISH_TODAY);
        if (cached != null) return attachSpecs(cached);
        LambdaQueryWrapper<Dish> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Dish::getIsToday, 1).eq(Dish::getStatus, 1).orderByDesc(Dish::getSales);
        List<Dish> list = dishMapper.selectList(wrapper);
        redisTemplate.opsForValue().set(RedisConstants.DISH_TODAY, list, RedisConstants.CACHE_TTL, TimeUnit.MINUTES);
        return attachSpecs(list);
    }

    @Override
    public List<DishWithSpecsVO> listAll(Long categoryId) {
        LambdaQueryWrapper<Dish> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Dish::getStatus, 1).orderByDesc(Dish::getSales);
        if (categoryId != null) wrapper.eq(Dish::getCategoryId, categoryId);
        return attachSpecs(dishMapper.selectList(wrapper));
    }
}
