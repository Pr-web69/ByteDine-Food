package com.lexiang.server.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lexiang.server.entity.OrderDetail;
import org.springframework.stereotype.Service;

/**
 * 订单明细 Service 接口
 * 继承 MyBatis-Plus 的 IService，自带 saveBatch/removeBatch 等批量操作
 */
public interface OrderDetailService extends IService<OrderDetail> {

}
