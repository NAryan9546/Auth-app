package com.pro.auth.Auth_app_backend.controllers;

import com.pro.auth.Auth_app_backend.Security.CookieService;
import com.pro.auth.Auth_app_backend.Security.JwtService;
import com.pro.auth.Auth_app_backend.dtos.LoginRequest;
import com.pro.auth.Auth_app_backend.dtos.TokenResponse;
import com.pro.auth.Auth_app_backend.dtos.UserDto;
import com.pro.auth.Auth_app_backend.entities.RefreshToken;
import com.pro.auth.Auth_app_backend.entities.User;
import com.pro.auth.Auth_app_backend.repositories.RefreshTokenRepository;
import com.pro.auth.Auth_app_backend.repositories.UserRepository;
import com.pro.auth.Auth_app_backend.services.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
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
import java.util.UUID;

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

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(

            @RequestBody LoginRequest loginRequest,
            HttpServletResponse response
    ) {
        //authenticate
        Authentication authenticate = authenticate( loginRequest );
        User user = userRepository.findByEmail( loginRequest.email( ) ).orElseThrow( ( ) -> new BadCredentialsException( "Invalid Username or password" ) );
        if ( !user.isEnabled( ) ) {
            throw new DisabledException( ( "User is disabled" ) );
        }

        String jti= UUID.randomUUID().toString();
        var refreshTokenA= RefreshToken.builder()
                .jti(jti)
                .user(user)
                .createdAt(Instant.now())
                .expiredAt(Instant.now().plusSeconds(jwtService.getRefreshTtlseconds()))
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshTokenA);

        // access Token--generate
        String accessToken = jwtService.generateAccessToken( user );
        String refreshToken = jwtService.generateRefreshToken(user, refreshTokenA.getJti());

        //use cookie service to attach refresh token in cookie

        cookieService.attachRefreshCookie(response,refreshToken, (int)jwtService.getRefreshTtlseconds());
        cookieService.addNoStoreHeaders(response);

        TokenResponse tokenResponse = TokenResponse.of( accessToken, refreshToken, jwtService.getAccessTtlSeconds( ), modelMapper.map( user, UserDto.class ) );
        return ResponseEntity.ok( tokenResponse );

    }

    private Authentication authenticate( LoginRequest loginRequest ) {
        try {

            return authenticationManager.authenticate( new UsernamePasswordAuthenticationToken( loginRequest.email( ), loginRequest.password( ) ) );

        } catch (Exception e) {
            throw new BadCredentialsException( "Invalid Username or password !!" );
        }
    }


    @PostMapping("/register")
    public ResponseEntity<UserDto> registerUser( @RequestBody UserDto userDto ) {
        return ResponseEntity.status( HttpStatus.CREATED ).body( authService.registerUser( userDto ) );
    }
}
