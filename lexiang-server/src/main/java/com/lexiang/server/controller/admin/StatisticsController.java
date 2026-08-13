package com.lexiang.server.controller.admin;
import com.lexiang.common.result.Result;
import com.lexiang.server.service.StatisticsService;
import com.lexiang.server.vo.StatisticsVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController("statisticsController")
@RequestMapping("/api/admin/statistics")
@RequiredArgsConstructor
public class StatisticsController {
    private final StatisticsService statisticsService;
    private final RedisTemplate<String, Object> redisTemplate;
    private static final String CACHE_KEY = "statistics:dashboard";
    private static final long CACHE_TTL = 5;

    @GetMapping("/dashboard")
    public Result<StatisticsVO> dashboard() {
        try { StatisticsVO c = (StatisticsVO) redisTemplate.opsForValue().get(CACHE_KEY); if (c != null) return Result.success(c); }
        catch (Exception e) { log.warn("redis down"); }
        StatisticsVO vo = statisticsService.getDashboard();
        try { redisTemplate.opsForValue().set(CACHE_KEY, vo, CACHE_TTL, TimeUnit.MINUTES); } catch (Exception ignored) {}
        return Result.success(vo);
    }

    @PostMapping("/refresh")
    public Result<Void> refresh() {
        try { redisTemplate.delete(CACHE_KEY); } catch (Exception ignored) {}
        return Result.success();
    }
}
