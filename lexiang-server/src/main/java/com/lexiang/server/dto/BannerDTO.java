package com.lexiang.server.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 轮播图新增/编辑 DTO
 */
@Data
public class BannerDTO {
    @NotBlank(message = "标题不能为空")
    private String title;

    @NotBlank(message = "图片URL不能为空")
    private String imageUrl;

    private String linkUrl;
    private Integer sortOrder;
    private Integer status;
}