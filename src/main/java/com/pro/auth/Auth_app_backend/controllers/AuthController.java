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
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import static java.util.spi.ToolProvider.findFirst;

import static org.aspectj.asm.internal.ProgramElement.trim;

@RestController
@RequestMapping("/api/v1/auth")
@AllArgsConstructor
public class AuthController {
    private final AuthService authService;
    private RefreshTokenRepository refreshTokenRepository;
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;

    private final ModelMapper modelMapper;

    private final JwtService jwtService;
    private final CookieService cookieService;

    // access and refresh token renew karne ke lieye api
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
        refreshTokenRepository.findByJti(jti).orElseThrow(()->new BadCredentialsException("Invalid Refresh Token"));



    }

    private Optional<String> readRefreshTokenFromRequest(RefreshTokenRequest body, HttpServletRequest request) {
        // 1. prefer reading refresh token from cookie
        if (request.getCookies() != null) {
            Optional<String> fromCookie = Arrays.stream(request.getCookies())
                    .filter(c -> cookieService.getRefreshTokenCookieName().equals(c.getName()))
                    .map(Cookie::getValue)
                    .filter(v -> v != null && !v.isBlank())
                    .findFirst();

            if (fromCookie.isPresent()) {
                return fromCookie;
            }
        }

        // 2. Fallback to request body if cookie is missing
        if (body != null && body.refreshToken() != null && !body.refreshToken().isBlank()) {
            return Optional.of(body.refreshToken());
        }

        // 3. Custom header
        String refreshHeader = request.getHeader("X-Refresh-Token");
        if (refreshHeader != null && !refreshHeader.isBlank()) {
            return Optional.of(refreshHeader.trim());
        }

        // 4. Authorization = Bearer <token>
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
            String candidate = authHeader.substring(7).trim();
            if (!candidate.isEmpty()) {
                try {
                    if (jwtService.isRefreshToken(candidate)) {
                        return Optional.of(candidate);
                    }
                } catch (Exception ignored) {
                    // Log error if necessary
                }
            }
        }

        return Optional.empty();
    }



    @PostMapping("/register")
    public ResponseEntity<UserDto> registerUser( @RequestBody UserDto userDto ) {
        return ResponseEntity.status( HttpStatus.CREATED ).body( authService.registerUser( userDto ) );
    }
}
