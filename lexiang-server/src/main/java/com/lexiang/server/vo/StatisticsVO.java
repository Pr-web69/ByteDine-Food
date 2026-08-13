package com.lexiang.server.vo;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class StatisticsVO {
    private BigDecimal todayRevenue;
    private Long todayOrders;
    private Long totalOrders;
    private BigDecimal totalRevenue;
    private List<Map<String, Object>> orderTrend;
    private List<Map<String, Object>> topDishes;
    private List<Map<String, Object>> orderStatus;
    private List<Map<String, Object>> salesReport;
}
