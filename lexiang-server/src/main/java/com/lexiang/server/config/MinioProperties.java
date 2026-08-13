package com.lexiang.server.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * MinIO 对象存储配置属性
 * <p>
 * 对应 application.yaml 中的 minio.* 配置。
 * 通过 {@code @ConfigurationProperties} 自动绑定，支持本地/容器多环境覆盖。
 */
@Data
@Component
@ConfigurationProperties(prefix = "minio")
public class MinioProperties {

    /** MinIO 服务端地址（后端 SDK 连接用，容器内为 http://minio:9000） */
    private String endpoint = "http://localhost:9000";

    /** 访问密钥 */
    private String accessKey = "minioadmin";

    /** 私密密钥 */
    private String secretKey = "minioadmin";

    /** 存储桶名称 */
    private String bucket = "lexiang-food";

    /**
     * 前端可访问的公网/宿主机地址（用于拼接返回给前端的图片 URL）
     * 与 endpoint 区分：容器内部用 endpoint，浏览器访问用 publicEndpoint
     */
    private String publicEndpoint = "http://localhost:9000";
}
