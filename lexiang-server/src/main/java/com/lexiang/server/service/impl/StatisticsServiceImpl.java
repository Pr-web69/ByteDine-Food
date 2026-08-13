package com.lexiang.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lexiang.server.entity.Dish;
import com.lexiang.server.entity.Orders;
import com.lexiang.server.mapper.DishMapper;
import com.lexiang.server.mapper.OrderDetailMapper;
import com.lexiang.server.mapper.OrdersMapper;
import com.lexiang.server.service.StatisticsService;
import com.lexiang.server.vo.StatisticsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * 数据统计 Service 实现
 *
 * 优化要点：
 * 1. getOrderTrend：GROUP BY DATE(create_time) 下沉到 SQL，只返回 7 条数据
 * 2. getOrderStatus：一条 SQL GROUP BY status，避免 5 次循环查询
 * 3. getTopDishes：基于 dish.sales 字段（V2.0 改为查询 order_detail 实时汇总）
 */
@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private static final String[] STATUS_NAMES = {"待支付", "已支付", "配送中", "已完成", "已取消"};

    private final OrdersMapper ordersMapper;
    private final OrderDetailMapper orderDetailMapper;
    private final DishMapper dishMapper;

    @Override
    public StatisticsVO getDashboard() {
        StatisticsVO vo = new StatisticsVO();
        LocalDate today = LocalDate.now();

        // 【业务规则】营业额仅统计已支付有效订单（status IN (1,2,3)）
        // 已取消(4)和待支付(0)的订单不计入营收

        // 今日营业额 & 今日订单数（仅有效订单）
        QueryWrapper<Orders> todayQ = new QueryWrapper<>();
        todayQ.select("COUNT(*) as cnt", "COALESCE(SUM(total_amount),0) as amt")
              .ge("create_time", today.atStartOfDay())
              .in("status", 1, 2, 3); // 仅已支付/配送中/已完成
        Map<String, Object> todayMap = ordersMapper.selectMaps(todayQ).get(0);
        vo.setTodayOrders(((Number)todayMap.get("cnt")).longValue());
        vo.setTodayRevenue(new BigDecimal(todayMap.get("amt").toString()));

        // 总营业额 & 总订单数（仅有效订单）
        QueryWrapper<Orders> allQ = new QueryWrapper<>();
        allQ.select("COUNT(*) as cnt", "COALESCE(SUM(total_amount),0) as amt")
            .in("status", 1, 2, 3);
        Map<String, Object> allMap = ordersMapper.selectMaps(allQ).get(0);
        vo.setTotalOrders(((Number)allMap.get("cnt")).longValue());
        vo.setTotalRevenue(new BigDecimal(allMap.get("amt").toString()));

        vo.setOrderTrend(getOrderTrend());
        vo.setTopDishes(getTopDishes());
        vo.setOrderStatus(getOrderStatus());
        return vo;
    }

    /**
     * 近7天每日订单数（SQL 聚合）
     * SELECT DATE(create_time) as date, COUNT(*) as count
     * FROM orders
     * WHERE create_time >= ?
     * GROUP BY DATE(create_time)
     * ORDER BY date
     */
    private List<Map<String, Object>> getOrderTrend() {
        QueryWrapper<Orders> wrapper = new QueryWrapper<>();
        wrapper.select("DATE(create_time) as date", "COUNT(*) as count")
               .ge("create_time", LocalDate.now().minusDays(6).atStartOfDay())
               .groupBy("DATE(create_time)")
               .orderByAsc("date");

        List<Map<String, Object>> dbResult = ordersMapper.selectMaps(wrapper);

        // 补全没有订单的日期（保证前端折线图连续）
        Map<String, Long> dateCount = new LinkedHashMap<>();
        for (int i = 6; i >= 0; i--) {
            dateCount.put(LocalDate.now().minusDays(i).toString(), 0L);
        }
        for (Map<String, Object> row : dbResult) {
            String date = row.get("date").toString();
            dateCount.put(date, ((Number) row.get("count")).longValue());
        }

        List<Map<String, Object>> result = new ArrayList<>();
        dateCount.forEach((date, count) -> {
            Map<String, Object> item = new HashMap<>();
            item.put("date", date);
            item.put("count", count);
            result.add(item);
        });
        return result;
    }

    /**
     * 菜品销量 TOP10（取自 dish.sales 字段）
     */
    private List<Map<String, Object>> getTopDishes() {
        QueryWrapper<Dish> wrapper = new QueryWrapper<>();
        wrapper.select("name", "sales", "price")
               .orderByDesc("sales")
               .last("LIMIT 10");

        List<Map<String, Object>> rows = dishMapper.selectMaps(wrapper);
        return rows.isEmpty() ? Collections.emptyList() : rows;
    }

    /**
     * 各状态订单数量统计（一条 SQL GROUP BY status）
     * SELECT status, COUNT(*) as value FROM orders GROUP BY status
     */
    private List<Map<String, Object>> getOrderStatus() {
        QueryWrapper<Orders> wrapper = new QueryWrapper<>();
        wrapper.select("status", "COUNT(*) as value")
               .groupBy("status");
        List<Map<String, Object>> dbResult = ordersMapper.selectMaps(wrapper);

        // 确保 5 种状态都返回（没有订单的状态 count=0）
        Map<Integer, Long> statusCount = new LinkedHashMap<>();
        for (int i = 0; i < 5; i++) statusCount.put(i, 0L);
        for (Map<String, Object> row : dbResult) {
            Integer status = ((Number) row.get("status")).intValue();
            Long value = ((Number) row.get("value")).longValue();
            statusCount.put(status, value);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        statusCount.forEach((status, count) -> {
            Map<String, Object> item = new HashMap<>();
            item.put("name", STATUS_NAMES[status]);
            item.put("value", count);
            result.add(item);
        });
        return result;
    }
}
