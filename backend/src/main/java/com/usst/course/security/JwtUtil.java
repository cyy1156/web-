package com.usst.course.security;

import com.usst.course.common.BizException;
import com.usst.course.common.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 签发与解析。使用 HMAC-SHA256，密钥长度不低于 32 字节。
 */
@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expireMillis;

    public JwtUtil(@Value("${app.jwt.secret}") String secret,
                   @Value("${app.jwt.expire-hours:168}") long expireHours) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expireMillis = expireHours * 3600_000L;
    }

    public String create(LoginUser user) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(user.userId()))
                .claim("username", user.username())
                .claim("role", user.role())
                .claim("refId", user.refId())
                .claim("name", user.name())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expireMillis))
                .signWith(key)
                .compact();
    }

    public LoginUser parse(String token) {
        try {
            Claims c = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            Object refId = c.get("refId");
            return new LoginUser(
                    Long.valueOf(c.getSubject()),
                    c.get("username", String.class),
                    c.get("role", String.class),
                    refId == null ? null : ((Number) refId).longValue(),
                    c.get("name", String.class));
        } catch (Exception e) {
            throw BizException.of(ErrorCode.UNAUTHORIZED);
        }
    }
}
