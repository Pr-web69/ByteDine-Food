package com.lexiang.server.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lexiang.common.exception.BusinessException;
import com.lexiang.server.dto.LoginDTO;
import com.lexiang.server.entity.Merchant;
import com.lexiang.server.entity.Employee;
import com.lexiang.server.mapper.EmployeeMapper;
import com.lexiang.server.mapper.MerchantMapper;
import com.lexiang.server.service.MerchantService;
import com.lexiang.server.interceptor.JwtInterceptor;
import com.lexiang.server.util.JwtUtil;
import com.lexiang.server.vo.LoginVO;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;


@Slf4j // 注入日志对象log，用于打印业务日志、异常信息
@Service // 将当前类注册为Spring业务层Bean，交由容器管理
@RequiredArgsConstructor// 通过构造器自动注入final修饰的成员变量，替代@Autowired
public class MerchantServiceImpl implements MerchantService {
    private final JwtUtil jwtUtil;
    private final MerchantMapper merchantMapper;
    private final EmployeeMapper employeeMapper;
    private final BCryptPasswordEncoder passwordEncoder;   // Spring Security 密码加密

    @Override
    public LoginVO login(LoginDTO loginDTO) {
        String username = loginDTO.getPhone();
        String password = loginDTO.getPassword();

        // 1. 先尝试商家账号登录
        LambdaQueryWrapper<Merchant> mw = new LambdaQueryWrapper<>();
        mw.eq(Merchant::getUsername, username);
        Merchant merchant = merchantMapper.selectOne(mw);
        if (merchant != null) {
            if (merchant.getStatus() == 0) throw new BusinessException(400, "账号已禁用");
            if (!passwordEncoder.matches(password, merchant.getPassword())) throw new BusinessException("用户名或密码错误");
            Map<String, Object> claims = new HashMap<>();
            claims.put("userId", merchant.getId());
            claims.put("userName", merchant.getStoreName());
            claims.put("type", 2);
            String token = jwtUtil.createToken(claims, String.valueOf(merchant.getId()));
            return new LoginVO(token, merchant.getId(), merchant.getUsername(), 2);
        }

        // 2. 再尝试员工账号登录 (employee table)
        LambdaQueryWrapper<Employee> ew = new LambdaQueryWrapper<>();
        ew.eq(Employee::getUsername, username);
        Employee emp = employeeMapper.selectOne(ew);
        if (emp == null) throw new BusinessException("用户名或密码错误");
        if (emp.getStatus() == null || emp.getStatus() == 0) throw new BusinessException(400, "账号已被禁用，请联系商家");
        // Employee uses MD5 (from EmployeeServiceImpl)
        String md5Pwd = org.springframework.util.DigestUtils.md5DigestAsHex(password.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        if (!md5Pwd.equals(emp.getPassword())) throw new BusinessException("用户名或密码错误");

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", emp.getMerchantId()); // use merchantId for data isolation
        claims.put("userName", emp.getUsername());
        claims.put("employeeId", emp.getId());
        claims.put("type", 2);
        String token = jwtUtil.createToken(claims, String.valueOf(emp.getId()));
        return new LoginVO(token, emp.getMerchantId(), emp.getUsername(), 2);
    }

        /**
         * 管理员重置商家密码
         * 用 BCrypt 加密新密码后更新入库
         */
    @Override
    public void resetPassword(String username, String newPassword) {
            LambdaQueryWrapper<Merchant> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Merchant::getUsername, username);
            Merchant merchant = merchantMapper.selectOne(wrapper);
            if (merchant == null) {
                throw new BusinessException(400, "商家不存在");
            }
            String encryptedPwd = passwordEncoder.encode(newPassword);
        merchant.setPassword(encryptedPwd);
        merchantMapper.updateById(merchant);
    }

    /** 固定验证码（本地测试用） */
    private static final String RESET_CODE = "666666";

    /**
     * 忘记密码：重置商家密码为 888888
     * 校验验证码通过后只更新密码字段
     */
    @Override
    public void forgotPassword(String username, String code) {
        if (username == null || username.isBlank()) {
            throw new BusinessException(400, "用户名不能为空");
        }
        if (!RESET_CODE.equals(code)) {
            throw new BusinessException(400, "验证码错误");
        }
        LambdaQueryWrapper<Merchant> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Merchant::getUsername, username);
        Merchant merchant = merchantMapper.selectOne(wrapper);
        if (merchant == null) {
            throw new BusinessException(400, "商家账号不存在");
        }
        merchant.setPassword(passwordEncoder.encode("888888"));
        merchantMapper.updateById(merchant);
        log.info("商家密码已重置:{}", username);
    }

    /**
     * 发送重置密码验证码（固定 666666，控制台打印）
     */
    @Override
    public void sendResetCode(String username) {
        if (username == null || username.isBlank()) {
            throw new BusinessException(400, "用户名不能为空");
        }
        LambdaQueryWrapper<Merchant> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Merchant::getUsername, username);
        if (merchantMapper.selectOne(wrapper) == null) {
            throw new BusinessException(400, "商家账号不存在");
        }
        log.info("========== 【短信验证码】商家用户名={} 的验证码为 {}（请复制到前端） ==========", username, RESET_CODE);
    }

    private Long getCurrentMerchantId() {
        Claims claims = JwtInterceptor.USER_HOLDER.get();
        if (claims == null) throw new BusinessException(401, "请先登录");
        return claims.get("userId", Long.class);
    }

    @Override
    public Integer getBusinessStatus() {
        Long merchantId = getCurrentMerchantId();
        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant == null) throw new BusinessException(400, "商家不存在");
        return merchant.getBusinessStatus() != null ? merchant.getBusinessStatus() : 1;
    }

    @Override
    public Integer toggleBusinessStatus() {
        Long merchantId = getCurrentMerchantId();
        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant == null) throw new BusinessException(400, "商家不存在");
        Integer newStatus = merchant.getBusinessStatus() != null && merchant.getBusinessStatus() == 1 ? 0 : 1;
        merchant.setBusinessStatus(newStatus);
        merchantMapper.updateById(merchant);
        return newStatus;
    }
}
