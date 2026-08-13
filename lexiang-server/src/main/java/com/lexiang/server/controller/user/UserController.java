package com.lexiang.server.controller.user;
import com.lexiang.common.result.Result;
import com.lexiang.server.dto.LoginDTO;
import com.lexiang.server.dto.RegisterDTO;
import com.lexiang.server.service.UserService;
import com.lexiang.server.vo.LoginVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 用户端接口 - 登录注册
 * 接口路径前缀：/api/user
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 用户登录
     * POST /api/user/login
     * @param loginDTO 手机号 + 密码，@Valid触发参数校验
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO loginDTO) {
        LoginVO loginVO = userService.login(loginDTO);
        return Result.success(loginVO);
    }

    /**
     * 用户注册
     * POST /api/user/register
     * @param registerDTO 手机号 + 密码 + 昵称(选填)
     */
    @PostMapping("/register")
    public Result<?> register(@Valid @RequestBody RegisterDTO registerDTO) {
        userService.register(registerDTO);
        return Result.success();
    }

    /**
     * 发送重置密码验证码（固定 666666，控制台打印）
     * POST /api/user/send-code
     * Body: { "phone": "13800138000" }
     */
    @PostMapping("/send-code")
    public Result<?> sendCode(@RequestBody Map<String, String> body) {
        userService.sendResetCode(body.get("phone"));
        return Result.success();
    }

    /**
     * 忘记密码：校验验证码后重置用户密码为 123456
     * POST /api/user/forgot-password
     * Body: { "phone": "13800138000", "code": "666666" }
     */
    @PostMapping("/forgot-password")
    public Result<?> forgotPassword(@RequestBody Map<String, String> body) {
        userService.resetPassword(body.get("phone"), body.get("code"));
        return Result.success();
    }
}