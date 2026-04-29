package com.pro.auth.Auth_app_backend.Security;

import com.pro.auth.Auth_app_backend.entities.Role;
import com.pro.auth.Auth_app_backend.entities.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class JwtService {


    private final SecretKey key;
    private final long accessTtlSeconds;
    private final long refreshTtlseconds;
    private final String issuer;

    public JwtService(
            @Value("${security.Jwt.secret") String secret,
            @Value("${security.jwt.access-tl-seconds}") long accessTtlSeconds,
            @Value("${security.jwt.refresh-tl-seconds}") long refreshTtlSeconds,
            @Value("${security.jwt.issuer}") String issuer
    ) {
        if (secret == null || secret.length() < 64) {
            throw new IllegalArgumentException("Invalid secret");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTtlSeconds = accessTtlSeconds;
        this.refreshTtlseconds = refreshTtlSeconds;
        this.issuer = issuer;
    }

    //generate token:
    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        List<String> roles = user.getRoles() == null ? List.of() :
                user.getRoles().stream().map(Role::getName).toList();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(user.getId().toString())
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(accessTtlSeconds)))
                .claims(Map.of(
                        "email", user.getEmail(),
                        "roles", roles,
                        "typ", "access"
                ))
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();


    }
    //generate refreshToken
    public String generateRefreshToken(User user,String jti) {
        Instant now = Instant.now();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(user.getId().toString())
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(refreshTtlseconds)))
                .claim("typ","refresh")
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();


    }
    //parse the token
    public Jws<Claims> parse(String token) {
        try {
            return Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
        } catch (JwtException e) {
            throw e;
        }
    }
    public boolean isAccessToken(String token){
        Claims c= parse(token).getPayload();
        return "access".equals(c.get("tpy"));
    }

    public boolean isRefreshToken(String token){
        Claims c= parse(token).getPayload();
        return "access".equals(c.get("tpy"));
    }

    public UUID getUserId(String token) {
        Claims c = parse(token).getPayload();
        return UUID.fromString(c.getSubject());
    }

    public String getJti(String token){
        return parse(token).getPayload().getId();
    }


}
