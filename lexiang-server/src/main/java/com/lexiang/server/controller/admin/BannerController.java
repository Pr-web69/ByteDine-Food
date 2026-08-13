package com.lexiang.server.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lexiang.common.result.Result;
import com.lexiang.server.dto.BannerDTO;
import com.lexiang.server.entity.Banner;
import com.lexiang.server.service.BannerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 后台 - 轮播图管理接口
 * /api/admin/banner
 */
@RestController("adminBannerController")
@RequestMapping("/api/admin/banner")
@RequiredArgsConstructor
public class BannerController {

    private final BannerService bannerService;

    @GetMapping("/list")
    public Result<Page<Banner>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(bannerService.pageQuery(page, pageSize));
    }

    @PostMapping
    public Result<?> add(@Valid @RequestBody BannerDTO dto) {
        bannerService.add(dto);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id,
                            @Valid @RequestBody BannerDTO dto) {
        bannerService.update(id, dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        bannerService.delete(id);
        return Result.success();
    }

    @PutMapping("/{id}/status")
    public Result<?> updateStatus(@PathVariable Long id,
                                  @RequestParam Integer status) {
        bannerService.updateStatus(id, status);
        return Result.success();
    }
}