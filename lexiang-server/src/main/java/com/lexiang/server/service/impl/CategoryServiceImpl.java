package com.lexiang.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lexiang.common.exception.BusinessException;
import com.lexiang.server.dto.CategoryDTO;
import com.lexiang.server.entity.Category;
import com.lexiang.server.interceptor.JwtInterceptor;
import com.lexiang.server.mapper.CategoryMapper;
import com.lexiang.server.service.CategoryService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 分类业务层实现
 * 商家后台管理菜品分类：增删改查 + 启禁用
 *
 * 关键设计：从 ThreadLocal 获取当前登录商家ID，
 * 确保商家只能管理自己店铺的分类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;

    /**
     * 从JWT拦截器存入的 ThreadLocal 中获取当前登录用户信息
     * 每次请求进来，拦截器解析Token后把用户信息存到 USER_HOLDER
     */
    private Claims getCurrentUser() {
        Claims claims = JwtInterceptor.USER_HOLDER.get();
        if (claims == null) {
            throw new BusinessException(401, "请先登录");
        }
        return claims;
    }

    // ==================== 分页查询分类 ====================

    @Override
    public Page<Category> pageQuery(Integer page, Integer pageSize, String name) {
        Page<Category> pageInfo = new Page<>(page, pageSize);

        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(name), Category::getName, name);
        wrapper.orderByAsc(Category::getSortOrder);

        categoryMapper.selectPage(pageInfo, wrapper);
        return pageInfo;
    }

    // ==================== 用户端查询所有启用分类 ====================

    @Override
    public List<Category> listAllActive() {
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Category::getStatus, 1);
        wrapper.orderByAsc(Category::getSortOrder);
        return categoryMapper.selectList(wrapper);
    }

    // ==================== 新增分类 ====================

    @Override
    public void add(CategoryDTO categoryDTO) {
        Claims claims = getCurrentUser();
        Long merchantId = claims.get("userId", Long.class);

        // 检查是否已有同名分类
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Category::getName, categoryDTO.getName())
                .eq(Category::getMerchantId, merchantId);
        if (categoryMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(400, "分类名称已存在");
        }

        Category category = new Category();
        category.setName(categoryDTO.getName());
        category.setSortOrder(categoryDTO.getSortOrder() != null ? categoryDTO.getSortOrder() : 0);
        category.setMerchantId(merchantId);
        category.setStatus(1);
        categoryMapper.insert(category);
    }

    // ==================== 修改分类 ====================

    @Override
    public void update(Long id, CategoryDTO categoryDTO) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException(400, "分类不存在");
        }

        category.setName(categoryDTO.getName());
        if (categoryDTO.getSortOrder() != null) {
            category.setSortOrder(categoryDTO.getSortOrder());
        }
        categoryMapper.updateById(category);
    }

    // ==================== 删除分类 ====================

    @Override
    public void delete(Long id) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException(400, "分类不存在");
        }
        categoryMapper.deleteById(id);
    }

    // ==================== 启禁用分类 ====================

    @Override
    public void updateStatus(Long id, Integer status) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException(400, "分类不存在");
        }

        category.setStatus(status);
        categoryMapper.updateById(category);
    }
}
