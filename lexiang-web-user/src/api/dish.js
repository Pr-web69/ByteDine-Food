import request from '@/utils/request'

/**
 * 获取热门菜品
 */
export const getHotDishes = () =>
    request.get('/user/dish/hot')

/**
 * 获取今日推荐菜品
 */
export const getTodayDishes = () =>
    request.get('/user/dish/today')

/**
 * 按分类获取全部在售菜品
 */
export const listAllDishes = (categoryId) =>
    request.get('/user/dish/list', { params: { categoryId } })