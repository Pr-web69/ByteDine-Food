package com.lexiang.server.controller.admin;

import com.lexiang.common.result.Result;
import com.lexiang.server.service.MinioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 后台 - 文件上传接口
 * <p>
 * 商家上传菜品图、banner 图等，文件存储到 MinIO，返回可访问 URL。
 * 该接口受 JwtInterceptor 保护，需携带商家 token。
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/upload")
@RequiredArgsConstructor
public class UploadController {

    private final MinioService minioService;

    /**
     * 上传图片到 MinIO
     *
     * @param file 上传的图片文件（表单字段名 file）
     * @return 图片可访问 URL（存数据库 image/imageUrl 字段）
     */
    @PostMapping("/image")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file) {
        String url = minioService.upload(file);
        return Result.success(url);
    }
}
