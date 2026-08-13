package com.lexiang.server.service;

import com.lexiang.common.exception.BusinessException;
import com.lexiang.server.config.MinioProperties;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * MinIO 文件存储服务
 * <p>
 * 负责将上传的文件（菜品图、banner 图、头像等）存储到 MinIO 对象存储，
 * 并返回前端可直接访问的 URL。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MinioService {

    private final MinioClient minioClient;
    private final MinioProperties props;

    /**
     * 上传文件到 MinIO
     * <p>
     * 对象命名规则：yyyy/MM/dd/{uuid}.{ext}，按日期分目录，UUID 保证唯一不冲突。
     *
     * @param file 上传的文件（MultipartFile）
     * @return 前端可直接访问的完整 URL
     */
    public String upload(MultipartFile file) {
        // 1. 空文件校验
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "上传文件不能为空");
        }

        // 2. 提取扩展名（从原始文件名）
        String originalName = file.getOriginalFilename();
        String ext = "";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf(".")).toLowerCase();
        }

        // 3. 生成对象名：yyyy/MM/dd/uuid.ext
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String objectName = datePath + "/" + UUID.randomUUID().toString().replace("-", "") + ext;

        // 4. 上传到 MinIO
        try {
            String contentType = file.getContentType() != null
                    ? file.getContentType()
                    : "application/octet-stream";
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(props.getBucket())
                            .object(objectName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(contentType)
                            .build()
            );
            log.info("[MinIO] 上传成功：object={}, size={}B", objectName, file.getSize());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[MinIO] 上传失败：{}", e.getMessage(), e);
            throw new BusinessException(500, "文件上传失败，请检查 MinIO 服务是否启动");
        }

        // 5. 返回前端可访问的完整 URL
        return props.getPublicEndpoint() + "/" + props.getBucket() + "/" + objectName;
    }
}
