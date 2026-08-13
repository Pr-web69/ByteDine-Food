import request from '@/utils/request'

/** 创建支付宝支付，返回支付页面 HTML */
export const createPayment = (orderId) =>
    request.post('/user/pay/create', { orderId })

/** 查询支付状态（轮询用）——超时放宽，避免后端主动查询支付宝时被 10s 默认超时切断并触发重试 */
export const queryPayment = (orderId) =>
    request.get('/user/pay/query', { params: { orderId }, timeout: 20000 })

/** 通过订单号查询支付状态（支付完成页跳转后使用） */
export const queryPaymentByOrderNo = (orderNo) =>
    request.get('/user/pay/queryNo', { params: { orderNo }, timeout: 20000 })