package com.lexiang.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lexiang.server.entity.Category;
import org.apache.ibatis.annotations.Mapper;
/**
 * 分类 Mapper
 * 继承 MyBatis-Plus 的 BaseMapper，自带通用 CRUD 方法：
 *   insert()、deleteById()、updateById()、selectById()、selectList() 等
 * 自定义复杂查询可在此接口声明方法，配合 XML 或注解实现
 */
@Mapper
public interface CategoryMapper extends BaseMapper<Category> {

}
