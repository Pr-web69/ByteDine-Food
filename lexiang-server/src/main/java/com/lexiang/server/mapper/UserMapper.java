package com.lexiang.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lexiang.server.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper
 * 继承 MyBatis-Plus 的 BaseMapper，自带常用的 CRUD 方法：
 *   insert()、deleteById()、updateById()、selectById()、selectList()
 *
 * 自定义 SQL 可写在这里，或在对应的 XML 文件中写
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
