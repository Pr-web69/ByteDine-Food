package com.lexiang.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lexiang.common.constant.RedisConstants;
import com.lexiang.common.exception.BusinessException;
import com.lexiang.server.dto.BannerDTO;
import com.lexiang.server.entity.Banner;
import com.lexiang.server.mapper.BannerMapper;
import com.lexiang.server.service.BannerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 轮播图 Service 实现
 * 开启的轮播图用 Redis 缓存，减少数据库查询
 * 后台增删改操作后主动清缓存，保证数据一致性（Cache-Aside 模式）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BannerServiceImpl implements BannerService {

    private final BannerMapper bannerMapper;

    private void safeDeleteCache(String key) {
        try { redisTemplate.delete(key); } catch (Exception e) { log.warn("Redis delete failed for {}: {}", key, e.getMessage()); }
    }
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 前台查询启用的轮播图（带缓存）
     * 先查 Redis，没有再查 MySQL，查完写入 Redis
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<Banner> listEnabled() {
        // 1. 尝试从 Redis 取
        List<Banner> cached = (List<Banner>) redisTemplate.opsForValue()
                .get(RedisConstants.BANNER_LIST);
        if (cached != null) {
            return cached;
        }

        // 2. Redis 没命中，查数据库
        LambdaQueryWrapper<Banner> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Banner::getStatus, 1)
                .orderByAsc(Banner::getSortOrder);
        List<Banner> list = bannerMapper.selectList(wrapper);

        // 3. 写入 Redis（即使空列表也缓存，防缓存穿透）
        redisTemplate.opsForValue().set(RedisConstants.BANNER_LIST, list,
                RedisConstants.CACHE_TTL, TimeUnit.MINUTES);
        return list;
    }

    @Override
    public Page<Banner> pageQuery(Integer page, Integer pageSize) {
        Page<Banner> pageInfo = new Page<>(page, pageSize);
        LambdaQueryWrapper<Banner> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(Banner::getSortOrder);
        bannerMapper.selectPage(pageInfo, wrapper);
        return pageInfo;
    }

    @Override
    public void add(BannerDTO dto) {
        Banner banner = new Banner();
        banner.setTitle(dto.getTitle());
        banner.setImageUrl(dto.getImageUrl());
        banner.setLinkUrl(dto.getLinkUrl());
        banner.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        banner.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        bannerMapper.insert(banner);
        // 清除缓存，下次查询重新加载
        safeDeleteCache(RedisConstants.BANNER_LIST);
    }

    @Override
    public void update(Long id, BannerDTO dto) {
        Banner banner = bannerMapper.selectById(id);
        if (banner == null) {
            throw new BusinessException(400, "轮播图不存在");
        }
        banner.setTitle(dto.getTitle());
        banner.setImageUrl(dto.getImageUrl());
        banner.setLinkUrl(dto.getLinkUrl());
        if (dto.getSortOrder() != null) banner.setSortOrder(dto.getSortOrder());
        if (dto.getStatus() != null) banner.setStatus(dto.getStatus());
        bannerMapper.updateById(banner);
        safeDeleteCache(RedisConstants.BANNER_LIST);
    }

    @Override
    public void delete(Long id) {
        Banner banner = bannerMapper.selectById(id);
        if (banner == null) {
            throw new BusinessException(400, "轮播图不存在");
        }
        bannerMapper.deleteById(id);
        safeDeleteCache(RedisConstants.BANNER_LIST);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        Banner banner = bannerMapper.selectById(id);
        if (banner == null) {
            throw new BusinessException(400, "轮播图不存在");
        }
        banner.setStatus(status);
        bannerMapper.updateById(banner);
        safeDeleteCache(RedisConstants.BANNER_LIST);
    }
}