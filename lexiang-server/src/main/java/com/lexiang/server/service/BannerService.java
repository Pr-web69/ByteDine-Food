package com.lexiang.server.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lexiang.server.dto.BannerDTO;
import com.lexiang.server.entity.Banner;
import java.util.List;

/**
 * 轮播图 Service 接口
 */
public interface BannerService {
    /** 前台查询启用的轮播图（带Redis缓存） */
    List<Banner> listEnabled();

    /** 后台分页查询 */
    Page<Banner> pageQuery(Integer page, Integer pageSize);

    /** 新增轮播图 */
    void add(BannerDTO dto);

    /** 修改轮播图 */
    void update(Long id, BannerDTO dto);

    /** 删除轮播图 */
    void delete(Long id);

    /** 启禁用 */
    void updateStatus(Long id, Integer status);
}