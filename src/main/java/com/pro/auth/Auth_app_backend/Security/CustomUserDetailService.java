package com.pro.auth.Auth_app_backend.Security;

import com.pro.auth.Auth_app_backend.entities.User;
import com.pro.auth.Auth_app_backend.exception.ResourceNotFoundException;
import com.pro.auth.Auth_app_backend.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

// This method will load the data from database
@Service
@RequiredArgsConstructor
public class CustomUserDetailService implements UserDetailsService {

    private final UserRepository userRepository;
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        User user = userRepository.findByEmail(username).orElseThrow(() -> new ResourceNotFoundException("Invalid Email or Password "));

        return user;
    }
}
