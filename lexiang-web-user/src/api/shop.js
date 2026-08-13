import request from '@/utils/request'

export const getShopStatus = () => request.get('/admin/shop/status')
export const toggleBusinessStatus = (open) => request.put(`/admin/shop/status?open=${open}`)
