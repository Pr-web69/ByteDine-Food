package com.lexiang.server.controller.admin;
import jakarta.validation.Valid;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lexiang.common.result.Result;
import com.lexiang.server.dto.CategoryDTO;
import com.lexiang.server.entity.Category;
import com.lexiang.server.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 商家后台 - 分类管理接口
 * /api/admin/category
 * @Valid 是 Jakarta Validation（JSR 380） 提供的参数校验注解，作用是触发对请求体对象的自动校验。
 */
@RestController
@RequestMapping("/api/admin/category")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService categoryService;
/**
 * 分页查询分类列表
 * GET /api/admin/category/list?page=1&pageSize=10&name=热菜
 */
@GetMapping("/list")
    public Result<Page<Category>> list(
            @RequestParam(defaultValue = "1")Integer page,
            @RequestParam(defaultValue = "10")Integer pageSize,
            @RequestParam(required = false) String name) {
    Page<Category> result = categoryService.pageQuery(page, pageSize, name);
    return Result.success(result);
}
    /**
     * 新增分类
     * POST /api/admin/category
     *
     * @param categoryDTO 分类名称 + 排序值，@Valid触发参数校验
     */
    @PostMapping
    public Result<?> add(@Valid @RequestBody CategoryDTO categoryDTO) {
        categoryService.add(categoryDTO);
        return Result.success();
    }

    /**
     * 修改分类
     * PUT /api/admin/category/1
     *
     * @param id          分类ID，从路径获取
     * @param categoryDTO 新的名称和排序值
     */
    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id,
                            @Valid @RequestBody CategoryDTO categoryDTO) {
        categoryService.update(id, categoryDTO);
        return Result.success();
    }

    /**
     * 删除分类
     * DELETE /api/admin/category/1
     *
     * @param id 分类ID
     */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return Result.success();
    }

    /**
     * 启禁用分类
     * PUT /api/admin/category/1/status
     *
     * @param id     分类ID
     * @param status 1启用 / 0禁用
     */
    @PutMapping("/{id}/status")
    public Result<?> updateStatus(@PathVariable Long id,
                                  @RequestParam Integer status) {
        categoryService.updateStatus(id, status);
        return Result.success();
    }
}
