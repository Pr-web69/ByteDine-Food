package com.lexiang.server.controller.user;

import com.lexiang.common.result.Result;
import com.lexiang.server.dto.AddressDTO;
import com.lexiang.server.entity.Address;
import com.lexiang.server.service.AddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * 用户收货地址控制器
 * 提供用户收货地址增删改查、获取默认地址、设置默认地址接口
 * 接口统一前缀：/api/user/address
 * 使用@Valid校验前端提交地址参数合法性
 */
@RestController // 标识为REST接口控制器，返回JSON格式数据
@RequestMapping("/api/user/address") // 接口统一请求路径前缀
@RequiredArgsConstructor // lombok构造注入，自动注入Service，替代@Autowired
public class AddressController {

    // 收货地址业务层
    private final AddressService addressService;

    /**
     * 查询当前登录用户全部收货地址列表
     * @return 统一返回封装结果，携带地址集合
     */
    @GetMapping("/list")
    public Result<List<Address>> list() {
        return Result.success(addressService.list());
    }

    /**
     * 获取当前用户的默认收货地址
     * @return 统一返回封装结果，携带默认地址实体
     */
    @GetMapping("/default")
    public Result<Address> getDefault() {
        return Result.success(addressService.getDefault());
    }

    /**
     * 新增收货地址
     * @param dto 前端提交地址表单参数，@Valid开启JSR参数校验
     * @return 无返回数据，仅返回成功标识
     */
    @PostMapping
    public Result<?> add(@Valid @RequestBody AddressDTO dto) {
        addressService.add(dto);
        return Result.success();
    }

    /**
     * 修改已有收货地址
     * @param id 待修改地址主键id（路径参数）
     * @param dto 修改后的地址表单参数，@Valid开启参数校验
     * @return 无返回数据，仅返回成功标识
     */
    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id,
                            @Valid @RequestBody AddressDTO dto) {
        addressService.update(id, dto);
        return Result.success();
    }

    /**
     * 删除指定收货地址
     * @param id 待删除地址主键id（路径参数）
     * @return 无返回数据，仅返回成功标识
     */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        addressService.delete(id);
        return Result.success();
    }

    /**
     * 将指定地址设置为用户默认收货地址
     * @param id 目标地址主键id（路径参数）
     * @return 无返回数据，仅返回成功标识
     */
    @PutMapping("/{id}/default")
    public Result<?> setDefault(@PathVariable Long id) {
        addressService.setDefault(id);
        return Result.success();
    }
}