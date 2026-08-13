import request from '@/utils/request'

// ── 规格分组 ──

export const getGroupList = () =>
  request.get('/admin/spec/group/list')

export const getGroupDetail = (id) =>
  request.get(`/admin/spec/group/${id}`)

export const addGroup = (data) =>
  request.post('/admin/spec/group', data)

export const updateGroup = (id, data) =>
  request.put(`/admin/spec/group/${id}`, data)

export const deleteGroup = (id) =>
  request.delete(`/admin/spec/group/${id}`)

// ── 规格选项 ──

export const addItem = (data) =>
  request.post('/admin/spec/item', data)

export const updateItem = (id, data) =>
  request.put(`/admin/spec/item/${id}`, data)

export const deleteItem = (id) =>
  request.delete(`/admin/spec/item/${id}`)

// ── 菜品关联 ──

export const saveDishGroups = (dishId, groupIds) =>
  request.put(`/admin/spec/dish/${dishId}/groups`, groupIds)

export const getDishGroups = (dishId) =>
  request.get(`/admin/spec/dish/${dishId}/groups`)

// ── 分类模板 ──

export const saveCategoryGroups = (categoryId, groupIds) =>
  request.put(`/admin/spec/category/${categoryId}/groups`, groupIds)

export const getCategoryGroups = (categoryId) =>
  request.get(`/admin/spec/category/${categoryId}/groups`)
