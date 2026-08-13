package com.lexiang.server.controller.user;

import com.lexiang.common.result.Result;
import com.lexiang.server.entity.Category;
import com.lexiang.server.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户端 - 分类接口
 * /api/category
 */
@RestController("userCategoryController")
@RequestMapping("/api/category")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * 查询所有启用状态的分类
     * GET /api/category/list
     */
    @GetMapping("/list")
    public Result<List<Category>> list() {
        List<Category> list = categoryService.listAllActive();
        return Result.success(list);
    }
}
