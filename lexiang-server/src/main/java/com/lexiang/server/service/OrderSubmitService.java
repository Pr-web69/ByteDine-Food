package com.lexiang.server.service;

import com.lexiang.server.dto.OrderSubmitDTO;
import com.lexiang.server.vo.OrderVO;

/**
 * 下单核心业务接口
 * 解决 @Transactional 自调用失效问题。
 * Spring AOP 基于代理，同类内 this.method() 不走代理，事务注解不生效。
 * OrderServiceImpl.submit() 通过注入调用本接口，事务正常生效。
 */
public interface OrderSubmitService {

    /**
     * 下单核心逻辑（事务保护）
     *
     * @param dto        下单参数（地址ID、备注）
     * @param userId     当前用户ID（由调用方从 JWT 上下文提取）
     * @param orderToken 幂等 Token（防重复提交，Lua 脚本原子校验+删除）
     * @return 订单视图（含订单号、金额、状态等）
     */
    OrderVO doSubmit(OrderSubmitDTO dto, Long userId, String orderToken);
}
