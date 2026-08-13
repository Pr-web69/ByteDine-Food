package com.lexiang.server.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lexiang.server.entity.OrderDetail;
import com.lexiang.server.mapper.OrderDetailMapper;
import com.lexiang.server.service.OrderDetailService;
import org.springframework.stereotype.Service;

/**
 * 订单明细 Service 实现类
 * ServiceImpl<Mapper, Entity> 是 MyBatis-Plus 提供的通用实现基类
 */
@Service
public class OrderDetailServiceImpl extends ServiceImpl<OrderDetailMapper, OrderDetail> implements OrderDetailService {

}
