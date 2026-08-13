import request from '@/utils/request'

/** 查询商家营业状态（用户端公开） */
export const getBusinessStatus = (merchantId) =>
  request.get('/user/merchant/business-status', { params: { merchantId } })

/** 商家端：查询当前营业状态 */
export const getAdminBusinessStatus = () =>
  request.get('/merchant/business-status')

/** 商家端：切换营业状态 */
export const toggleAdminBusinessStatus = () =>
  request.post('/merchant/business-status/toggle')
