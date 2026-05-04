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
        return request.getRequestURI().startsWith("/api/v1/auth");
    }



    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String header= request.getHeader("Authorization");
        if (header != null) {
            log.info("Authorization header: {}", header);
        }
        if(header!=null && header.startsWith("Bearer ")){

            //token extract and validate then authentication create and then set into security context
            String token = header.substring(7);
            try{

                Jws<Claims> parse =jwtService.parse(token);
                Claims payload = parse.getPayload();

                //check for access token
                if(!jwtService.isAccessToken(token)){
                    filterChain.doFilter(request,response);
                    return;
                }

                String userId = payload.getSubject();
                UUID userUuid = UserHelper.parseUUID(userId);

                userRepository.findById(userUuid)
                        .ifPresent(user->{

                                    //check for user enable or not
                                    if(user.isEnabled()) {
                                        List<GrantedAuthority> authorities = user.getRoles () == null ? List.of () : user.getRoles ().stream ()
                                                .map (role -> new SimpleGrantedAuthority (role.getName ())).collect (Collectors.toList ());
                                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken (
                                                user.getEmail (),
                                                null,
                                                authorities
                                        );

                                        authentication.setDetails (new WebAuthenticationDetailsSource ().buildDetails (request));
                                        //final line :to set the authentication to security context
                                        if (SecurityContextHolder.getContext ().getAuthentication () == null)
                                            SecurityContextHolder.getContext ().setAuthentication (authentication);
                                    }



                        });


            }catch (ExpiredJwtException e){
                request.setAttribute("error","Token Expired");
               // e.printStackTrace();

            } catch (Exception e){
                request.setAttribute("error"," Invalid Token");
                //e.printStackTrace();

            }
        }
        filterChain.doFilter(request,response);




    }
}
