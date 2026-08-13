package com.lexiang.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lexiang.server.entity.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface OrdersMapper extends BaseMapper<Orders> {
    /**
     * 增加菜品销量（支付成功后调用）
     * @param dishId   菜品ID
     * @param quantity 增加数量
     */
    @Update("UPDATE dish SET sales = sales + #{quantity} WHERE id = #{dishId}")
    void increaseSales(@Param("dishId") Long dishId, @Param("quantity") int quantity);

    /**
     * 查询用户当前未完成的待支付订单数量
     * <p>
     * 业务规则：一个用户同时只能有1条待支付订单。
     * 订单支付成功(1)、已取消(4)、超时自动关闭后，限制解除。
     *
     * @param userId 用户ID
     * @return 待支付订单数量（正常情况应为0或1）
     */
    @Select("SELECT COUNT(*) FROM orders WHERE user_id = #{userId} AND status = 0")
    int countUnpaidByUserId(@Param("userId") Long userId);
}
