package com.lexiang.server.service;

import com.lexiang.server.dto.LoginDTO;
import com.lexiang.server.dto.RegisterDTO;
import com.lexiang.server.vo.LoginVO;

/**
 * 用户业务接口
 */
public interface UserService {

    /**
     * 用户登录
     * 1. 根据手机号查询用户
     * 2. 校验密码（BCrypt）
     * 3. 生成JWT Token
     * @param loginDTO 手机号 + 密码
     * @return LoginVO（token + 用户信息）
     */
    LoginVO login(LoginDTO loginDTO);

    /**
     * 用户注册
     * 1. 检查手机号是否已注册
     * 2. BCrypt加密密码
     * 3. 保存用户信息
     */
    void register(RegisterDTO registerDTO);

    /**
     * 忘记密码：重置为 123456
     * 1. 校验固定验证码 666666
     * 2. 根据手机号查询用户
     * 3. 存在则重置密码为 123456（BCrypt），不影响其它数据
     */
    void resetPassword(String phone, String code);

    /**
     * 发送重置密码验证码（固定 666666，控制台打印，不接真实短信）
     */
    void sendResetCode(String phone);
}
