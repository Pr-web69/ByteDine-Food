package com.lexiang.server.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lexiang.server.dto.DishDTO;
import com.lexiang.server.dto.DishPageQueryDTO;
import com.lexiang.server.entity.Dish;
import com.lexiang.server.vo.DishWithSpecsVO;

import java.util.List;

public interface DishService {
    Page<Dish> pageQuery(DishPageQueryDTO queryDTO);
    void add(DishDTO dishDTO);
    void update(Long id, DishDTO dishDTO);
    void delete(Long id);
    void updateStatus(Long id, Integer status);
    List<DishWithSpecsVO> getHotDishes();
    List<DishWithSpecsVO> getTodayDishes();
    List<DishWithSpecsVO> listAll(Long categoryId);
}
