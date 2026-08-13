package com.lexiang.server.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lexiang.common.exception.BusinessException;
import com.lexiang.server.dto.LoginDTO;
import com.lexiang.server.dto.RegisterDTO;
import com.lexiang.server.entity.User;
import com.lexiang.server.mapper.UserMapper;
import com.lexiang.server.service.UserService;
import com.lexiang.server.util.JwtUtil;
import com.lexiang.server.vo.LoginVO;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.HashMap;

/**
 * 用户业务层实现类
 * 处理用户登录、注册相关业务逻辑，依赖Mapper完成数据库操作、JwtUtil生成登录令牌
 */
@Slf4j // 注入日志对象log，用于打印业务日志、异常信息
@Service // 将当前类注册为Spring业务层Bean，交由容器管理
@RequiredArgsConstructor // lombok注解，通过构造器自动注入final修饰的成员变量，替代@Autowired
public class UserServiceImpl implements UserService {
    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder;   // Spring Security 密码加密

    /**
     * 用户登录接口
     *
     * @param loginDTO 登录请求参数封装对象：账号、密码等登录信息
     * @return LoginVO 登录成功返回视图对象，包含token、用户基础信息
     */
    @Override
    public LoginVO login(LoginDTO loginDTO) {
        //1.查询用户
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getPhone, loginDTO.getPhone());
        User user = userMapper.selectOne(wrapper);
        if (user == null) {
            throw new BusinessException(400, "手机号或密码错误");
        }
        //2.检查账号状态
        if ((user.getStatus() == 0)) {
            throw new BusinessException(400, "账号已禁用");
        }
        //3.校验密码
        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            throw new BusinessException(400, "手机号或密码错误");
        }
        //4.生成JWT Token
        HashMap<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("userName", user.getNickname());
        claims.put("type", 1);
        String token = jwtUtil.createToken(claims, String.valueOf(user.getId()));
        log.info("用户登录成功:{}", user.getPhone());
        return new LoginVO(token, user.getId(), user.getNickname(), 1);
    }

        /**
         * 用户注册接口
         * @param registerDTO 注册请求参数封装对象：用户名、密码、手机号等注册信息
         */
        @Override
        public void register (RegisterDTO registerDTO){
            //1.检查手机号是否已注册
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getPhone, registerDTO.getPhone());
            User existUser = userMapper.selectOne(wrapper);
            if (existUser != null) {
                throw new BusinessException(400, "手机号已注册");
            }
            //2.加密密码
            String encryptedPassword = passwordEncoder.encode(registerDTO.getPassword());
            //3.构建用户对象
            User user1 = new User();
            user1.setPhone(registerDTO.getPhone());
            user1.setPassword(encryptedPassword);
            // 昵称默认："用户" + 手机后4位
            if ((registerDTO.getNickname() == null || registerDTO.getNickname().isBlank())) {
                String phone = registerDTO.getPhone();
                user1.setNickname("用户" + phone.substring(phone.length() - 4));
            }else {
                user1.setNickname(registerDTO.getNickname());
            }
            user1.setStatus(1);
            //4.插入数据库
            userMapper.insert(user1);
            log.info("用户注册成功:{}", registerDTO.getPhone());
        }

        /** 固定验证码（本地测试用，不接真实短信） */
        private static final String RESET_CODE = "666666";

        /**
         * 忘记密码：重置为 123456
         * 校验验证码通过后只更新密码字段，不动其它数据
         */
        @Override
        public void resetPassword(String phone, String code) {
            if (phone == null || phone.isBlank()) {
                throw new BusinessException(400, "手机号不能为空");
            }
            if (!RESET_CODE.equals(code)) {
                throw new BusinessException(400, "验证码错误");
            }
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getPhone, phone);
            User user = userMapper.selectOne(wrapper);
            if (user == null) {
                throw new BusinessException(400, "该手机号未注册");
            }
            user.setPassword(passwordEncoder.encode("123456"));
            userMapper.updateById(user);
            log.info("用户密码已重置:{}", phone);
        }

        /**
         * 发送重置密码验证码（固定 666666，控制台打印）
         */
        @Override
        public void sendResetCode(String phone) {
            if (phone == null || phone.isBlank()) {
                throw new BusinessException(400, "手机号不能为空");
            }
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getPhone, phone);
            if (userMapper.selectOne(wrapper) == null) {
                throw new BusinessException(400, "该手机号未注册");
            }
            log.info("========== 【短信验证码】手机号={} 的验证码为 {}（请复制到前端） ==========", phone, RESET_CODE);
        }
    }
