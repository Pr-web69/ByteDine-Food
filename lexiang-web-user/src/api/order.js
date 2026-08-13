import request from '@/utils/request'

/** 获取下单幂等 Token */
export const getOrderToken = () => request.get('/user/order/token')

/** 提交订单（带幂等 Token） */
export const submitOrder = (data, headers = {}) =>
    request.post('/user/order', data, { headers })

/**
 * 查询用户订单列表
 * @param params 分页、状态参数
 */
export const getOrderList = (params) =>
    request.get('/user/order/list', { params })

/**
 * 获取订单详情
 * @param id 订单id
 */
export const getOrderDetail = (id) =>
    request.get(`/user/order/${id}`)

/**
 * 取消订单
 * @param id 订单id
 * @param reason 取消原因
 */
export const cancelOrder = (id, reason) =>
    request.put(`/user/order/${id}/cancel`, { reason })

/**
 * 用户确认收货
 * @param id 订单id
 */
export const confirmOrder = (id) =>
    request.put(`/user/order/${id}/confirm`)

export const acceptOrder = (id) => request.put(`/admin/order/${id}/accept`)

/** 商家完成订单 */
export const completeOrder = (id) => request.put(`/admin/order/${id}/complete`)

/**
 * 商家获取订单详情
 * @param id 订单id
 */
export const getAdminOrderDetail = (id) => request.get(`/admin/order/${id}`)

/**
 * 商家取消订单
 * @param id 订单id
 * @param reason 取消原因
 */
export const adminCancelOrder = (id, reason) =>
    request.put(`/admin/order/${id}/cancel`, { reason })

/** 获取待处理订单数（商家端通知badge用） */
export const getOrderCount = (params) => request.get('/admin/order/list', { params })