package com.lexiang.server.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lexiang.common.exception.BusinessException;
import com.lexiang.server.entity.Employee;
import com.lexiang.server.interceptor.JwtInterceptor;
import com.lexiang.server.mapper.EmployeeMapper;
import com.lexiang.server.service.EmployeeService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {
    private final EmployeeMapper mapper;

    private Long getMerchantId() {
        Claims c = JwtInterceptor.USER_HOLDER.get();
        if (c == null) throw new BusinessException(401, "not login");
        return c.get("userId", Long.class);
    }

    @Override
    public Page<Employee> page(Integer page, Integer size, String keyword) {
        Page<Employee> p = new Page<>(page, size);
        Long mid = getMerchantId();
        LambdaQueryWrapper<Employee> w = new LambdaQueryWrapper<>();
        w.eq(Employee::getMerchantId, mid);
        if (keyword != null && !keyword.isEmpty()) w.like(Employee::getUsername, keyword);
        w.orderByDesc(Employee::getCreateTime);
        return mapper.selectPage(p, w);
    }

    @Override
    public void add(Employee emp) {
        Long mid = getMerchantId();
        if (mapper.selectCount(new LambdaQueryWrapper<Employee>().eq(Employee::getUsername, emp.getUsername()).eq(Employee::getMerchantId, mid)) > 0)
            throw new BusinessException(400, "username exists");
        emp.setMerchantId(mid);
        emp.setPassword(DigestUtils.md5DigestAsHex(emp.getPassword().getBytes(StandardCharsets.UTF_8)));
        if (emp.getRole() == null) emp.setRole("STAFF");
        if (emp.getStatus() == null) emp.setStatus(1);
        mapper.insert(emp);
    }

    @Override
    public void update(Employee emp) {
        Employee e = mapper.selectById(emp.getId());
        if (e == null || !e.getMerchantId().equals(getMerchantId())) throw new BusinessException(403, "no permission");
        if (emp.getPhone() != null) e.setPhone(emp.getPhone());
        if (emp.getRole() != null) e.setRole(emp.getRole());
        if (emp.getStatus() != null) e.setStatus(emp.getStatus());
        mapper.updateById(e);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        Employee e = mapper.selectById(id);
        if (e == null || !e.getMerchantId().equals(getMerchantId())) throw new BusinessException(403, "no permission");
        e.setStatus(status);
        mapper.updateById(e);
    }

    @Override
    public void delete(Long id) {
        Employee e = mapper.selectById(id);
        if (e == null || !e.getMerchantId().equals(getMerchantId())) throw new BusinessException(403, "no permission");
        mapper.deleteById(id);
    }

    @Override
    public void resetPassword(Long id, String newPwd) {
        Employee e = mapper.selectById(id);
        if (e == null || !e.getMerchantId().equals(getMerchantId())) throw new BusinessException(403, "no permission");
        e.setPassword(DigestUtils.md5DigestAsHex(newPwd.getBytes(StandardCharsets.UTF_8)));
        mapper.updateById(e);
    }

    @Override
    public List<Employee> listByMerchant(Long merchantId) {
        return mapper.selectList(new LambdaQueryWrapper<Employee>().eq(Employee::getMerchantId, merchantId).eq(Employee::getStatus, 1));
    }
}
