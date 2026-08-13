package com.lexiang.server.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SpecGroupDTO {
    @NotBlank(message = "分组名称不能为空")
    private String name;
    private Integer isRequired;    // 1=必选 0=可选
    private Integer maxSelect;     // 最多可选数量
    private Integer isExclusive;   // 1=互斥（同组仅选1项）0=可多选
    private Integer sortOrder;
    private Integer status;        // 1=启用 0=禁用
}
