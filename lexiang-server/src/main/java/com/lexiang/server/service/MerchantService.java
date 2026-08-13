package com.lexiang.server.service;

import com.lexiang.server.dto.LoginDTO;
import com.lexiang.server.vo.LoginVO;

/**
 * 商家业务接口
 */
public interface MerchantService {

    /**
     * 商家登录
     * 1. 根据用户名查询商家
     * 2. 校验密码（BCrypt）
     * 3. 生成JWT Token
     *
     * @param loginDTO 用户名 + 密码
     * @return LoginVO（token + 商家信息）
     */
    LoginVO login(LoginDTO loginDTO);


    /** 管理员重置商家密码 */
    void resetPassword(String username, String newPassword);

    /** 忘记密码：校验验证码后重置商家密码为 888888 */
    void forgotPassword(String username, String code);

    /** 发送重置密码验证码（固定 666666，控制台打印) */
    void sendResetCode(String username);

    /** 查询当前登录商家的营业状态 */
    Integer getBusinessStatus();

    /** 切换当前登录商家的营业状态 */
    Integer toggleBusinessStatus();
}
