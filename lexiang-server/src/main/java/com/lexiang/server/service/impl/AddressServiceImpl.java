package com.lexiang.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lexiang.common.exception.BusinessException;
import com.lexiang.server.dto.AddressDTO;
import com.lexiang.server.entity.Address;
import com.lexiang.server.interceptor.JwtInterceptor;
import com.lexiang.server.mapper.AddressMapper;
import com.lexiang.server.service.AddressService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 收货地址业务层实现
 *
 * 核心设计：
 * 1. 从 ThreadLocal 获取当前用户ID，保证数据隔离
 * 2. 默认地址唯一性：一个用户同时只有一个默认地址
 * 3. setDefault 先清除旧默认再设置新默认（原子操作思维）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final AddressMapper addressMapper;

    /**
     * 从JWT拦截器存入的ThreadLocal中取出当前登录用户ID
     * ThreadLocal 是线程级别的变量，每个请求一个线程，互不干扰
     */
    private Long getUserId() {
        Claims claims = JwtInterceptor.USER_HOLDER.get();
        if (claims == null) {
            throw new BusinessException(401, "请先登录");
        }
        return claims.get("userId", Long.class);
    }

    /**
     * 查询当前用户所有收货地址
     * 排序规则：默认地址排最前 → 按更新时间倒序
     * 这样前端展示时默认地址始终在第一位
     */
    @Override
    public List<Address> list() {
        LambdaQueryWrapper<Address> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Address::getUserId, getUserId())           // 只看自己的
                .orderByDesc(Address::getIsDefault)             // 默认地址排第一
                .orderByDesc(Address::getUpdateTime);           // 最近修改的靠前
        return addressMapper.selectList(wrapper);
    }

    /**
     * 获取当前用户的默认地址
     * 下单页用：自动填充收货地址
     */
    @Override
    public Address getDefault() {
        LambdaQueryWrapper<Address> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Address::getUserId, getUserId())
                .eq(Address::getIsDefault, 1);                  // isDefault=1 即默认
        return addressMapper.selectOne(wrapper);
    }

    /**
     * 新增收货地址
     *
     * 逻辑：
     * 1. 限制每个用户最多10个地址（业务约束）
     * 2. 如果是该用户的第一个地址 → 自动设为默认
     * 3. 非第一个地址 → isDefault=0
     */
    @Override
    public void add(AddressDTO dto) {
        Long userId = getUserId();

        // 统计当前用户已有的地址数量
        LambdaQueryWrapper<Address> countWrapper = new LambdaQueryWrapper<>();
        countWrapper.eq(Address::getUserId, userId);

        Long count = addressMapper.selectCount(countWrapper);
        if (count >= 10) {
            throw new BusinessException(400, "地址数量已达上限（最多10个）");
        }

        // 构建地址实体
        Address address = new Address();
        address.setUserId(userId);
        address.setContactName(dto.getContactName());
        address.setContactPhone(dto.getContactPhone());
        address.setAddressDetail(dto.getAddressDetail());
//         * count == 0  → 还没有地址 → isDefault = 1（默认）
//         * count > 0  → 已有地址  → isDefault = 0（非默认）
        address.setIsDefault(count == 0 ? 1 : 0);
        addressMapper.insert(address);
    }

    /**
     * 修改收货地址
     * 权限校验：只能改自己的地址
     */
    @Override
    public void update(Long id, AddressDTO dto) {
        Address address = addressMapper.selectById(id);
        if (address == null) {
            throw new BusinessException(400, "地址不存在");
        }
        // 权限校验：URL里的id可能被篡改，必须验证归属
        if (!address.getUserId().equals(getUserId())) {
            throw new BusinessException(403, "无权限操作该地址");
        }
        // 只更新允许修改的三个字段
        address.setContactName(dto.getContactName());
        address.setContactPhone(dto.getContactPhone());
        address.setAddressDetail(dto.getAddressDetail());
        addressMapper.updateById(address);
    }

    /**
     * 删除收货地址
     */
    @Override
    public void delete(Long id) {
        Address address = addressMapper.selectById(id);
        if (address == null) {
            throw new BusinessException(400, "地址不存在");
        }
        if (!address.getUserId().equals(getUserId())) {
            throw new BusinessException(403, "无权限操作该地址");
        }
        addressMapper.deleteById(id);
    }

    /**
     * 设置默认地址
     *
     * 核心逻辑（两步走）：
     * 第一步：把当前用户【所有】地址的 isDefault 改为 0
     *         → 清除旧的默认地址，保证不会出现两个默认
     * 第二步：把【指定】地址的 isDefault 改为 1
     *         → 设置新的默认地址
     *
     * 为什么分两步？因为 update(null, wrapper) 可以批量更新，
     * 避免先查旧默认再改的 N+1 问题
     */
    @Override
    public void setDefault(Long id) {
        Address address = addressMapper.selectById(id);
        if (address == null) {
            throw new BusinessException(400, "地址不存在");
        }
        if (!address.getUserId().equals(getUserId())) {
            throw new BusinessException(403, "无权限操作该地址");
        }

        /*
         * 第一步：批量清除
         * 构造条件：当前用户的所有地址
         * Address 对象只设 isDefault=0，其他字段为 null
         * MyBatis-Plus 的 update(entity, wrapper) 只更新非 null 字段
         */
        LambdaQueryWrapper<Address> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Address::getUserId, getUserId());

        Address updateAddr = new Address();
        updateAddr.setIsDefault(0);
        addressMapper.update(updateAddr, wrapper);  // SQL: UPDATE address SET is_default=0 WHERE user_id=?

        /*
         * 第二步：指定地址设为默认
         */
        address.setIsDefault(1);
        addressMapper.updateById(address);          // SQL: UPDATE address SET is_default=1 WHERE id=?
    }
}