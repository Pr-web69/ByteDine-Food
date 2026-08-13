package com.lexiang.server.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lexiang.server.entity.Employee;
import org.apache.ibatis.annotations.Mapper;
@Mapper
public interface EmployeeMapper extends BaseMapper<Employee> {}
