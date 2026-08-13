package com.lexiang.common.constant;

/**
 * Redis缓存Key常量类
 * 统一管理所有Redis的Key前缀，避免项目中出现魔法字符串
 * 万一Key格式要改，只改这里一处即可
 */
public class RedisConstants {

    /** 热门菜品缓存Key */
    public static final String DISH_HOT = "dish:hot";

    /** 今日推荐缓存Key */
    public static final String DISH_TODAY = "dish:today";

    /** 分类列表缓存Key */
    public static final String CATEGORY_LIST = "category:list";

    /** 轮播图缓存Key */
    public static final String BANNER_LIST = "banner:list";

    /** 下单防重Token前缀，使用时拼接用户ID：order:token:123 */
    public static final String ORDER_TOKEN = "order:token:";

    /** 用户购物车缓存前缀，使用时拼接用户ID：cart:user:123 */
    public static final String CART_USER = "cart:user:";

    /** Redis缓存默认过期时间：30分钟 */
    public static final Long CACHE_TTL = 30L;
}
