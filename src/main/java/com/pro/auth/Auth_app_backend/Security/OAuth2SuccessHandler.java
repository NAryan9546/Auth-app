package com.pro.auth.Auth_app_backend.Security;

import com.pro.auth.Auth_app_backend.entities.Provider;
import com.pro.auth.Auth_app_backend.entities.RefreshToken;
import com.pro.auth.Auth_app_backend.entities.User;
import com.pro.auth.Auth_app_backend.repositories.RefreshTokenRepository;
import com.pro.auth.Auth_app_backend.repositories.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
@AllArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final CookieService cookieService;
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        logger.info("Successful authentication");
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String registrationId = "Unknown";
        if(authentication instanceof OAuth2AuthenticationToken token){
            registrationId = token.getAuthorizedClientRegistrationId();
        }
        logger.info("registrationId: " + registrationId);

        User finalUser;
        switch(registrationId) {
            case "google" -> {
                String email = oAuth2User.getAttributes().getOrDefault("email", "").toString();
                String name = oAuth2User.getAttributes().getOrDefault("name", "").toString();
                String picture = oAuth2User.getAttributes().getOrDefault("picture", "").toString();

                // Check if user already exists in DB
                Optional<User> existingUser = userRepository.findByEmail(email);

                if (existingUser.isPresent()) {
                    logger.info("User already exists in database");
                    finalUser = existingUser.get();
                } else {
                    logger.info("New OAuth user. Saving to database...");
                    User newUser = User.builder()
                            .email(email)
                            .name(name)
                            .image(picture)
                            .provider(Provider.GOOGLE)
                            .build();
                    finalUser = userRepository.save(newUser);
                }
            }
            default -> {
                throw new RuntimeException("Invalid registration id: " + registrationId);
            }
        }

        // Token Issuance Logic Block
        String jti = UUID.randomUUID().toString();
        RefreshToken refreshTokenOb = RefreshToken.builder()
                .jti(jti)
                .user(finalUser)
                .revoked(false)
                .createdAt(Instant.now())
                .expiredAt(Instant.now().plusSeconds(jwtService.getRefreshTtlseconds()))
                .build();

        refreshTokenRepository.save(refreshTokenOb);

        String accessToken = jwtService.generateAccessToken(finalUser);
        String refreshToken = jwtService.generateRefreshToken(finalUser, refreshTokenOb.getJti());

        cookieService.attachRefreshCookie(response, refreshToken, (int) jwtService.getRefreshTtlseconds());

        response.getWriter().write("Login successful");
    }
}