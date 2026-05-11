package com.pro.auth.Auth_app_backend.controllers;

import com.pro.auth.Auth_app_backend.Security.CookieService;
import com.pro.auth.Auth_app_backend.Security.JwtService;
import com.pro.auth.Auth_app_backend.dtos.LoginRequest;
import com.pro.auth.Auth_app_backend.dtos.RefreshTokenRequest;
import com.pro.auth.Auth_app_backend.dtos.TokenResponse;
import com.pro.auth.Auth_app_backend.dtos.UserDto;
import com.pro.auth.Auth_app_backend.entities.RefreshToken;
import com.pro.auth.Auth_app_backend.entities.User;
import com.pro.auth.Auth_app_backend.repositories.RefreshTokenRepository;
import com.pro.auth.Auth_app_backend.repositories.UserRepository;
import com.pro.auth.Auth_app_backend.services.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@AllArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final JwtService jwtService;
    private final CookieService cookieService;

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody LoginRequest loginRequest, HttpServletResponse response) {
        // 1. Authenticate user
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.email(), loginRequest.password())
        );

        User user = (User) authentication.getPrincipal();

        // 2. Generate Tokens
        String accessToken = jwtService.generateAccessToken(user);

        // 3. Handle Refresh Token Rotation (Save to DB)
        String jti = UUID.randomUUID().toString();
        RefreshToken refreshTokenObj = RefreshToken.builder()
                .jti(jti)
                .user(user)
                .createdAt(Instant.now())
                .expiredAt(Instant.now().plusSeconds(jwtService.getRefreshTtlseconds()))
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshTokenObj);
        String refreshToken = jwtService.generateRefreshToken(user, jti);

        // 4. Set Cookie and Headers
        cookieService.attachRefreshCookie(response, refreshToken, (int) jwtService.getRefreshTtlseconds());
        cookieService.addNoStoreHeaders(response);

        // 5. Build Response
        TokenResponse tokenResponse = TokenResponse.of(
                accessToken,
                refreshToken,
                jwtService.getAccessTtlSeconds(),
                modelMapper.map(user, UserDto.class)
        );

        return ResponseEntity.ok(tokenResponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refreshToken(
            @RequestBody(required = false) RefreshTokenRequest body,
            HttpServletResponse response,
            HttpServletRequest request
    ) {
        String refreshToken = readRefreshTokenFromRequest(body, request)
                .orElseThrow(() -> new BadCredentialsException("Refresh Token is missing"));

        if (!jwtService.isRefreshToken(refreshToken)){
            throw new BadCredentialsException("Invalid Refresh Token Type");
        }

        String jti = jwtService.getJti(refreshToken);
        UUID userId = jwtService.getUserId(refreshToken);

        RefreshToken storedRefreshToken = refreshTokenRepository.findByJti(jti)
                .orElseThrow(() -> new BadCredentialsException("Invalid Refresh Token"));

        if(storedRefreshToken.isRevoked() || storedRefreshToken.getExpiredAt().isBefore(Instant.now())) {
            throw new BadCredentialsException("Refresh token is revoked or expired");
        }

        if(!storedRefreshToken.getUser().getId().equals(userId)) {
            throw new BadCredentialsException("Token mismatch");
        }

        // Rotate: Revoke old, create new
        storedRefreshToken.setRevoked(true);
        String newJti = UUID.randomUUID().toString();
        storedRefreshToken.setReplacedByToken(newJti);
        refreshTokenRepository.save(storedRefreshToken);

        User user = storedRefreshToken.getUser();

        RefreshToken newRefreshTokenObj = RefreshToken.builder()
                .jti(newJti)
                .user(user)
                .createdAt(Instant.now())
                .expiredAt(Instant.now().plusSeconds(jwtService.getRefreshTtlseconds()))
                .revoked(false)
                .build();

        refreshTokenRepository.save(newRefreshTokenObj);

        String newAccessToken = jwtService.generateAccessToken(user);
        String newRefreshToken = jwtService.generateRefreshToken(user, newJti);

        cookieService.attachRefreshCookie(response, newRefreshToken, (int) jwtService.getRefreshTtlseconds());
        cookieService.addNoStoreHeaders(response);

        return ResponseEntity.ok(TokenResponse.of(
                newAccessToken,
                newRefreshToken,
                jwtService.getAccessTtlSeconds(),
                modelMapper.map(user, UserDto.class))
        );
    }

    private Optional<String> readRefreshTokenFromRequest(RefreshTokenRequest body, HttpServletRequest request) {
        if (request.getCookies() != null) {
            Optional<String> fromCookie = Arrays.stream(request.getCookies())
                    .filter(c -> cookieService.getRefreshTokenCookieName().equals(c.getName()))
                    .map(Cookie::getValue)
                    .findFirst();
            if (fromCookie.isPresent()) return fromCookie;
        }

        if (body != null && body.refreshToken() != null && !body.refreshToken().isBlank()) {
            return Optional.of(body.refreshToken());
        }

        String refreshHeader = request.getHeader("X-Refresh-Token");
        if (refreshHeader != null) return Optional.of(refreshHeader.trim());

        return Optional.empty();
    }

    @PostMapping("/register")
    public ResponseEntity<UserDto> registerUser(@RequestBody UserDto userDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerUser(userDto));
    }
}