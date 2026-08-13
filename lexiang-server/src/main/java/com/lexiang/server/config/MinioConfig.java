package com.lexiang.server.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.SetBucketPolicyArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO 客户端配置
 * <p>
 * 关键设计：MinioClient 的 builder().build() 只创建对象、不发起网络请求，
 * 因此即使 MinIO 服务未启动，本 Bean 也能正常创建，不影响应用启动。
 * bucket 初始化用 try-catch 包裹，失败仅记录日志，不影响其他功能。
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class MinioConfig {

    private final MinioProperties props;

    /**
     * 创建 MinioClient 单例 Bean，并在启动时确保 bucket 存在 + 公开读
     */
    @Bean
    public MinioClient minioClient() {
        MinioClient client = MinioClient.builder()
                .endpoint(props.getEndpoint())
                .credentials(props.getAccessKey(), props.getSecretKey())
                .build();

        // 启动时初始化 bucket（服务未启动时静默失败，不阻塞应用启动）
        try {
            boolean exists = client.bucketExists(
                    BucketExistsArgs.builder().bucket(props.getBucket()).build()
            );
            if (!exists) {
                client.makeBucket(MakeBucketArgs.builder().bucket(props.getBucket()).build());
                // 设置公开读策略，让前端能直接通过 URL 访问图片
                String policy = "{\"Version\":\"2012-10-17\",\"Statement\":[{\"Effect\":\"Allow\","
                        + "\"Principal\":{\"AWS\":[\"*\"]},\"Action\":[\"s3:GetObject\"],"
                        + "\"Resource\":[\"arn:aws:s3:::" + props.getBucket() + "/*\"]}]}";
                client.setBucketPolicy(
                        SetBucketPolicyArgs.builder().bucket(props.getBucket()).config(policy).build()
                );
                log.info("[MinIO] 初始化成功：bucket={} 已创建并设置公开读", props.getBucket());
            } else {
                log.info("[MinIO] bucket={} 已存在，跳过初始化", props.getBucket());
            }
        } catch (Exception e) {
            log.warn("[MinIO] 初始化失败（MinIO 服务未启动时上传功能不可用，不影响其他功能）：{}",
                    e.getMessage());
        }
        return client;
    }
}
