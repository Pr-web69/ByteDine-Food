package com.lexiang.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lexiang.server.entity.Dish;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface DishMapper extends BaseMapper<Dish> {

    /**
     * 原子扣减库存（DB乐观锁，防超卖）
     * WHERE stock >= quantity 保证库存不足时返回0，不会扣成负数
     *
     * @return 受影响行数：1=成功，0=库存不足
     */
    @Update("UPDATE dish SET stock = stock - #{quantity} WHERE id = #{dishId} AND stock >= #{quantity}")
    int deductStock(@Param("dishId") Long dishId, @Param("quantity") Integer quantity);

    /**
     * 原子恢复库存（取消/超时回滚用）
     */
    @Update("UPDATE dish SET stock = stock + #{quantity} WHERE id = #{dishId}")
    int restoreStock(@Param("dishId") Long dishId, @Param("quantity") Integer quantity);
}
