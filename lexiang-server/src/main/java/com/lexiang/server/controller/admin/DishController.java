package com.lexiang.server.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lexiang.common.result.Result;
import com.lexiang.server.dto.DishDTO;
import com.lexiang.server.dto.DishPageQueryDTO;
import com.lexiang.server.entity.Dish;
import com.lexiang.server.service.DishService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 商家后台 - 菜品管理接口
 * /api/admin/dish
 */
@RestController("adminDishController")
@RequestMapping("/api/admin/dish")
@RequiredArgsConstructor
public class DishController {

    private final DishService dishService;

    /**
     * 分页查询菜品
     * GET /api/admin/dish/list?page=1&pageSize=10&name=宫保&categoryId=3&status=1&sortBy=sales&sortOrder=desc
     * 参数全部可选，不传则查全部
     */
    @GetMapping("/list")
    public Result<Page<Dish>> list(@Valid DishPageQueryDTO queryDTO) {
        Page<Dish> result = dishService.pageQuery(queryDTO);
        return Result.success(result);
    }

    /**
     * 新增菜品
     * POST /api/admin/dish
     */
    @PostMapping
    public Result<?> add(@Valid @RequestBody DishDTO dishDTO) {
        dishService.add(dishDTO);
        return Result.success();
    }

    /**
     * 修改菜品
     * PUT /api/admin/dish/1
     */
    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id,
                            @Valid @RequestBody DishDTO dishDTO) {
        dishService.update(id, dishDTO);
        return Result.success();
    }

    /**
     * 删除菜品
     * DELETE /api/admin/dish/1
     */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        dishService.delete(id);
        return Result.success();
    }

    /**
     * 上架/下架
     * PUT /api/admin/dish/1/status?status=0
     */
    @PutMapping("/{id}/status")
    public Result<?> updateStatus(@PathVariable Long id,
                                  @RequestParam Integer status) {
        dishService.updateStatus(id, status);
        return Result.success();
    }
}