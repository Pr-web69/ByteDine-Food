package com.lexiang.server.controller.admin;

import com.lexiang.common.result.Result;
import com.lexiang.server.dto.LoginDTO;
import com.lexiang.server.service.MerchantService;
import com.lexiang.server.vo.LoginVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController("merchantAdminController")
@RequestMapping("/api/merchant")
@RequiredArgsConstructor
public class MerchantController {

    private final MerchantService merchantService;

    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        LoginVO vo = merchantService.login(dto);
        return Result.success(vo);
    }

    /**
     * 管理员重置商家密码
     * Body: { "username": "admin", "newPassword": "654321" }
     */
    @PutMapping("/reset-password")
    public Result<?> resetPassword(@RequestBody Map<String, String> body) {
        merchantService.resetPassword(body.get("username"), body.get("newPassword"));
        return Result.success();
    }

    /**
     * 发送重置密码验证码（固定 666666，控制台打印）
     * Body: { "username": "admin" }
     */
    @PostMapping("/send-code")
    public Result<?> sendCode(@RequestBody Map<String, String> body) {
        merchantService.sendResetCode(body.get("username"));
        return Result.success();
    }

    /**
     * 忘记密码：校验验证码后重置商家密码为 888888
     * Body: { "username": "admin", "code": "666666" }
     */
    @PostMapping("/forgot-password")
    public Result<?> forgotPassword(@RequestBody Map<String, String> body) {
        merchantService.forgotPassword(body.get("username"), body.get("code"));
        return Result.success();
    }

    /**
     * 查询当前登录商家营业状态
     * GET /api/merchant/business-status
     */
    @GetMapping("/business-status")
    public Result<Integer> getBusinessStatus() {
        return Result.success(merchantService.getBusinessStatus());
    }

    /**
     * 切换当前登录商家营业状态
     * POST /api/merchant/business-status/toggle
     */
    @PostMapping("/business-status/toggle")
    public Result<Integer> toggleBusinessStatus() {
        return Result.success(merchantService.toggleBusinessStatus());
    }
}