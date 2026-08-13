package com.lexiang.server.controller.admin;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lexiang.common.result.Result;
import com.lexiang.server.entity.Employee;
import com.lexiang.server.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/employee")
@RequiredArgsConstructor
public class EmployeeController {
    private final EmployeeService service;

    @GetMapping("/page")
    public Result<Page<Employee>> page(@RequestParam(defaultValue = "1") Integer page,
                                        @RequestParam(defaultValue = "10") Integer size,
                                        @RequestParam(required = false) String keyword) {
        return Result.success(service.page(page, size, keyword));
    }

    @PostMapping
    public Result<Void> add(@RequestBody Employee emp) { service.add(emp); return Result.success(); }

    @PutMapping
    public Result<Void> update(@RequestBody Employee emp) { service.update(emp); return Result.success(); }

    @PutMapping("/{id}/status")
    public Result<Void> status(@PathVariable Long id, @RequestParam Integer status) { service.updateStatus(id, status); return Result.success(); }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) { service.delete(id); return Result.success(); }

    @PutMapping("/{id}/reset-pwd")
    public Result<Void> resetPwd(@PathVariable Long id, @RequestParam String password) { service.resetPassword(id, password); return Result.success(); }
}
