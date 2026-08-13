import request from '@/utils/request'

/**
 * 获取首页轮播图列表
 */
export const getBanners = () =>
    request.get('/user/banner/list')