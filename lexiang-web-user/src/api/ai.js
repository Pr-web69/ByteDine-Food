import request from '@/utils/request'

/** 用户端 AI 点餐推荐 */
export const aiSuggest = (question) => request.post('/user/ai/suggest', { question })

/** 用户端订单 AI 咨询 */
export const aiOrderConsult = (orderId, question) =>
  request.post('/user/ai/order-consult', { orderId: String(orderId), question })

/** 商家端 AI 经营查询 */
export const merchantAiQuery = (question) => request.post('/admin/ai/query', { question })
