package com.lexiang.server.service;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lexiang.server.entity.Employee;
import java.util.List;

public interface EmployeeService {
    Page<Employee> page(Integer page, Integer size, String keyword);
    void add(Employee emp);
    void update(Employee emp);
    void updateStatus(Long id, Integer status);
    void delete(Long id);
    void resetPassword(Long id, String newPwd);
    List<Employee> listByMerchant(Long merchantId);
}
