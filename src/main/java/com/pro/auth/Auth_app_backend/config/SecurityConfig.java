package com.pro.auth.Auth_app_backend.config;

import com.pro.auth.Auth_app_backend.Security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

import static org.springframework.security.authorization.SingleResultAuthorizationManager.permitAll;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private  JwtAuthenticationFilter jwtAuthenticationFilter;


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http.csrf ( AbstractHttpConfigurer::disable )
                .cors ( Customizer.withDefaults ( ) )
                .sessionManagement ( sm -> sm.sessionCreationPolicy ( SessionCreationPolicy.STATELESS ) )
                .authorizeHttpRequests ( authorizeHttpRequests ->
                        authorizeHttpRequests.requestMatchers ( "/api/v1/auth/register" ).permitAll ( )
                                .requestMatchers ( "/api/v1/auth/login" ).permitAll ( )
                                .anyRequest ( ).authenticated ( )
                )

                .exceptionHandling(configurer -> configurer.authenticationEntryPoint((request, response, e) -> {
                    e.printStackTrace();
                    response.setStatus(401);
                    response.setContentType("application/json");
                    String message="Unauthorized access"+e.getMessage();
                    Map<String,String> errorMap=Map.of("message",message,"statuscode",Integer.toString(401));
                    var objectMapper= new ObjectMapper();
                    response.getWriter().write(objectMapper.writeValueAsString(errorMap));
                }))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }
//    @Bean
//    public UserDetailsService users(){
//        User.UserBuilder userBuilder = User.withDefaultPasswordEncoder();
//
//        UserDetails user1 = userBuilder.username("Aryan").password("tyu").roles("ADMIN").build();
//        UserDetails user2 = userBuilder.username("Rohan").password("xyz").roles("ADMIN").build();
//        UserDetails user3 = userBuilder.username("Ayan").password("").roles("USER").build();
//        return new InMemoryUserDetailsManager(user1,user2,user3);
//
//    }

}
