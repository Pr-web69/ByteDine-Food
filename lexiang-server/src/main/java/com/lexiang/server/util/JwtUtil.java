package com.lexiang.server.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * JWT 工具类
 * 负责 Token 的生成、解析、校验
 * JWT 结构：Header.Payload.Signature
 * - Header：算法类型 HS256
 * - Payload：存放 userId、userName、type 等自定义字段
 * - Signature：用密钥对前两部分签名，防篡改
 *
 * 工作流程：
 * 1. 登录成功 → createToken() 生成 Token 返回给前端
 * 2. 前端请求 → 请求头带 Authorization: Bearer xxx
 * 3. 拦截器 → parseToken() 解析 Token 获取用户信息
 */
@Component
public class JwtUtil {

    /** JWT 签名密钥，从 application.yaml 读取 */
    @Value("${jwt.secret}")
    private String secret;

    /** Token 过期时间（单位：秒），默认7天 = 604800秒 */
    @Value("${jwt.expiration:604800}")
    private Long expirationSec;


    /**
     * 获取签名密钥
     * 使用 HMAC-SHA256 算法，密钥长度需要至少256位(32字节)
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 生成 JWT Token
     *
     * @param claims  自定义载荷（userId、userName、type 等）
     * @param subject 主题，通常放用户ID
     * @return JWT 字符串
     */
    public String createToken(Map<String, Object> claims, String subject) {
        // 秒换算为毫秒
        long expireTimeMs = System.currentTimeMillis() + expirationSec * 1000;
        return Jwts.builder()
                .claims(claims)                     // 自定义字段
                .subject(subject)                    // 主题=用户ID
                .issuedAt(new Date())                // 签发时间
                .expiration(new Date(expireTimeMs)) // 过期时间
                .signWith(getSigningKey())           // 签名
                .compact();                          // 生成字符串
    }

    /**
     * 解析 Token，获取所有 Claims
     *
     * @param token JWT字符串
     * @return Claims 对象，可通过 get("键名") 获取自定义字段
     * @throws io.jsonwebtoken.JwtException Token过期/签名错误/格式错误时抛出
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 判断 Token 是否过期
     *
     * @param token JWT字符串
     * @return true=已过期 false=未过期
     */
    public boolean isExpired(String token) {
        try {
            return parseToken(token).getExpiration().before(new Date());
        } catch (Exception e) {
            return true; // 解析异常也视为过期
        }
    }
}
