package com.lexiang.server.service;

import com.lexiang.server.dto.CategoryDTO;
import com.lexiang.server.entity.Category;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

public interface CategoryService {
    /**
     * 分页查询分类列表
     * @param page     当前页码
     * @param pageSize 每页条数
     * @param name     分类名称（模糊搜索，可选）
     * @return 分页对象，包含分类列表和分页信息
     */
    Page<Category> pageQuery(Integer page, Integer pageSize, String name);

    /**
     * 查询所有启用的分类（用户端展示）
     * @return 分类列表
     */
    List<Category> listAllActive();

    /** 新增分类 */
    void add(CategoryDTO categoryDTO);

    /** 修改分类 */
    void update(Long id, CategoryDTO categoryDTO);

    /** 删除分类 */
    void delete(Long id);

    /** 启禁用分类 */
    void updateStatus(Long id, Integer status);
}
