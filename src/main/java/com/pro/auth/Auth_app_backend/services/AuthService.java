package com.pro.auth.Auth_app_backend.services;

import com.pro.auth.Auth_app_backend.dtos.UserDto;

public interface AuthService {
    UserDto registerUser( UserDto userDto);
}
