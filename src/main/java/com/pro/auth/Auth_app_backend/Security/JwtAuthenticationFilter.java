package com.pro.auth.Auth_app_backend.Security;

import com.pro.auth.Auth_app_backend.entities.User;
import com.pro.auth.Auth_app_backend.helpers.UserHelper;
import com.pro.auth.Auth_app_backend.repositories.UserRepository;
import io.jsonwebtoken.*;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;


@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        // Skips filtering for login and register endpoints
        return request.getRequestURI().startsWith("/api/v1/auth");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);
        try {
            // 1. Parse and validate the token signature
            Jws<Claims> parse = jwtService.parse(token);
            Claims payload = parse.getPayload();

            // 2. Ensure it is an Access Token, not a Refresh Token
            if (!jwtService.isAccessToken(token)) {
                log.warn("Refresh token used as Access token");
                filterChain.doFilter(request, response);
                return;
            }

            String userId = payload.getSubject();
            UUID userUuid = UserHelper.parseUUID(userId);

            // 3. Find user and set Authentication
            userRepository.findById(userUuid).ifPresent(user -> {
                if (user.isEnabled()) {
                    List<SimpleGrantedAuthority> authorities = user.getRoles() == null ? List.of() :
                            user.getRoles().stream()
                                    .map(role -> new SimpleGrantedAuthority(role.getName()))
                                    .collect(Collectors.toList());

                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            user.getEmail(),
                            null,
                            authorities
                    );

                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            });

        } catch (ExpiredJwtException e) {
            log.error("JWT token is expired: {}", e.getMessage());
            request.setAttribute("error", "Token Expired");
            // Important: In a production app, you might want to send a 401 response here
        } catch (Exception e) {
            log.error("JWT token validation failed: {}", e.getMessage());
            request.setAttribute("error", "Invalid Token");
        }

        filterChain.doFilter(request, response);
    }
}