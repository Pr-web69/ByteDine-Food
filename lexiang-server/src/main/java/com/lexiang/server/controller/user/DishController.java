package com.lexiang.server.controller.user;

import com.lexiang.common.result.Result;
import com.lexiang.server.service.DishService;
import com.lexiang.server.vo.DishWithSpecsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController("userDishController")
@RequestMapping("/api/user/dish")
@RequiredArgsConstructor
public class DishController {

    private final DishService dishService;

    @GetMapping("/hot")
    public Result<List<DishWithSpecsVO>> hot() {
        return Result.success(dishService.getHotDishes());
    }

    @GetMapping("/today")
    public Result<List<DishWithSpecsVO>> today() {
        return Result.success(dishService.getTodayDishes());
    }

    @GetMapping("/list")
    public Result<List<DishWithSpecsVO>> list(@RequestParam(required = false) Long categoryId) {
        return Result.success(dishService.listAll(categoryId));
    }
}
