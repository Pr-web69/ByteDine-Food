package com.lexiang.server.controller.admin;

import com.lexiang.common.result.Result;
import com.lexiang.server.dto.SpecGroupDTO;
import com.lexiang.server.dto.SpecItemDTO;
import com.lexiang.server.service.SpecService;
import com.lexiang.server.vo.SpecGroupVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商家后台 - 规格管理接口
 * /api/admin/spec
 */
@RestController
@RequestMapping("/api/admin/spec")
@RequiredArgsConstructor
public class SpecController {

    private final SpecService specService;

    /* ================================
       规格分组 CRUD
       ================================ */

    /** 查询所有规格分组（含选项） */
    @GetMapping("/group/list")
    public Result<List<SpecGroupVO>> listGroups() {
        return Result.success(specService.listAllGroups());
    }

    /** 查询单个规格分组详情 */
    @GetMapping("/group/{id}")
    public Result<SpecGroupVO> getGroupDetail(@PathVariable Long id) {
        return Result.success(specService.getGroupDetail(id));
    }

    /** 新增规格分组 */
    @PostMapping("/group")
    public Result<Long> addGroup(@Valid @RequestBody SpecGroupDTO dto) {
        return Result.success(specService.addGroup(dto));
    }

    /** 修改规格分组 */
    @PutMapping("/group/{id}")
    public Result<?> updateGroup(@PathVariable Long id, @Valid @RequestBody SpecGroupDTO dto) {
        specService.updateGroup(id, dto);
        return Result.success();
    }

    /** 删除规格分组（级联删除选项和关联） */
    @DeleteMapping("/group/{id}")
    public Result<?> deleteGroup(@PathVariable Long id) {
        specService.deleteGroup(id);
        return Result.success();
    }

    /* ================================
       规格选项 CRUD
       ================================ */

    /** 新增规格选项 */
    @PostMapping("/item")
    public Result<Long> addItem(@Valid @RequestBody SpecItemDTO dto) {
        return Result.success(specService.addItem(dto));
    }

    /** 修改规格选项 */
    @PutMapping("/item/{id}")
    public Result<?> updateItem(@PathVariable Long id, @Valid @RequestBody SpecItemDTO dto) {
        specService.updateItem(id, dto);
        return Result.success();
    }

    /** 删除规格选项 */
    @DeleteMapping("/item/{id}")
    public Result<?> deleteItem(@PathVariable Long id) {
        specService.deleteItem(id);
        return Result.success();
    }

    /* ================================
       菜品-规格关联
       ================================ */

    /** 绑定菜品规格组 */
    @PutMapping("/dish/{dishId}/groups")
    public Result<?> bindDishSpecs(@PathVariable Long dishId, @RequestBody List<Long> groupIds) {
        specService.bindDishSpecs(dishId, groupIds);
        return Result.success();
    }

    /** 查询菜品已关联的规格组ID */
    @GetMapping("/dish/{dishId}/groups")
    public Result<List<Long>> getDishGroupIds(@PathVariable Long dishId) {
        return Result.success(specService.getDishGroupIds(dishId));
    }

    /* ================================
       分类-规格模板
       ================================ */

    /** 绑定分类规格模板 */
    @PutMapping("/category/{categoryId}/groups")
    public Result<?> bindCategorySpecs(@PathVariable Long categoryId, @RequestBody List<Long> groupIds) {
        specService.bindCategorySpecs(categoryId, groupIds);
        return Result.success();
    }

    /** 查询分类关联的规格组ID */
    @GetMapping("/category/{categoryId}/groups")
    public Result<List<Long>> getCategoryGroupIds(@PathVariable Long categoryId) {
        return Result.success(specService.getCategoryGroupIds(categoryId));
    }
}
