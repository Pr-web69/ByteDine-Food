package com.lexiang.server.controller.user;

import com.lexiang.common.result.Result;
import com.lexiang.server.entity.Merchant;
import com.lexiang.server.mapper.MerchantMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户端 - 商家公开信息接口
 * /api/user/merchant
 */
@RestController("userMerchantController")
@RequestMapping("/api/user/merchant")
@RequiredArgsConstructor
public class MerchantController {

    private final MerchantMapper merchantMapper;

    /**
     * 查询商家营业状态（公开接口，无需登录）
     * GET /api/user/merchant/business-status?merchantId=1
     * 不传 merchantId 时默认查询 id=1 的商家
     */
    @GetMapping("/business-status")
    public Result<Integer> businessStatus(@RequestParam(required = false) Long merchantId) {
        if (merchantId == null) merchantId = 1L;
        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant == null) {
            return Result.success(0);
        }
        Integer status = merchant.getBusinessStatus();
        return Result.success(status != null ? status : 1);
    }
}
