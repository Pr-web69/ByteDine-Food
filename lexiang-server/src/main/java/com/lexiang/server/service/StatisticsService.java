package com.lexiang.server.service;

import com.lexiang.server.vo.StatisticsVO;

/**
 * 为商家仪表盘 ECharts 图表提供数据
 */
public interface StatisticsService {

    /** 获取仪表盘全部统计数据 */
    StatisticsVO getDashboard();
}