package com.lexiang.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lexiang.server.entity.Merchant;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商家 Mapper
 * 继承 MyBatis-Plus 的 BaseMapper，自带常用的 CRUD 方法
 */
@Mapper
public interface MerchantMapper extends BaseMapper<Merchant> {
}
