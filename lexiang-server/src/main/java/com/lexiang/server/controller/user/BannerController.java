package com.lexiang.server.controller.user;

import com.lexiang.common.result.Result;
import com.lexiang.server.entity.Banner;
import com.lexiang.server.service.BannerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户端 - 首页轮播图（无需登录即可访问）
 * /api/user/banner/list
 */
@RestController("userBannerController")
@RequestMapping("/api/user/banner")
@RequiredArgsConstructor
public class BannerController {

    private final BannerService bannerService;

    /**
     * 前台查询启用的轮播图
     */
    @GetMapping("/list")
    public Result<List<Banner>> list() {
        return Result.success(bannerService.listEnabled());
    }
}