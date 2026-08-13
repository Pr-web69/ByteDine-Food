import request from '@/utils/request'

/**
 * 查询收货地址列表
 */
export const getAddressList = () =>
    request.get('/user/address/list')

/**
 * 新增收货地址
 * @param data 地址表单信息
 */
export const addAddress = (data) =>
    request.post('/user/address', data)

/**
 * 设置默认收货地址
 * @param id 地址id
 */
export const setDefaultAddress = (id) =>
    request.put(`/user/address/${id}/default`)

/**
 * 删除收货地址
 * @param id 地址id
 */
export const updateAddress = (id, data) =>
    request.put(`/user/address/${id}`, data)

export const deleteAddress = (id) =>
    request.delete(`/user/address/${id}`)