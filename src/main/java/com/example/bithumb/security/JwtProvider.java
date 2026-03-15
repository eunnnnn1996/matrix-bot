package com.example.bithumb.security;

import java.security.Key;
import java.util.Date;

import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtProvider {

    private final String SECRET_KEY = "bithumb-secret-key-bithumb-secret-key-bithumb";
    private final long ACCESS_TOKEN_VALID = 1000 * 60 * 30;   // 30분
    private final long REFRESH_TOKEN_VALID = 1000 * 60 * 60 * 24 * 7; // 7일

    private final Key key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());

    // AccessToken 생성
    public String createAccessToken(Long userId, String role) {

        Claims claims = Jwts.claims().setSubject(String.valueOf(userId));
        claims.put("role", role);

        Date now = new Date();
        Date expiry = new Date(now.getTime() + ACCESS_TOKEN_VALID);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // RefreshToken 생성
    public String createRefreshToken(Long userId) {

        Date now = new Date();
        Date expiry = new Date(now.getTime() + REFRESH_TOKEN_VALID);

        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // userId 추출
    public Long getUserId(String token) {

        return Long.parseLong(
                Jwts.parserBuilder()
                        .setSigningKey(key)
                        .build()
                        .parseClaimsJws(token)
                        .getBody()
                        .getSubject()
        );
    }
}