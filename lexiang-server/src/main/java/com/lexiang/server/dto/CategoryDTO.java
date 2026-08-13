package com.lexiang.server.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 分类新增/修改请求DTO
 */
@Data
public class CategoryDTO {
    @NotBlank (message = "分类名不能为空")
    private String name;
    private Integer sortOrder;
}
