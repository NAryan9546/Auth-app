package com.pro.auth.Auth_app_backend.services;

import com.pro.auth.Auth_app_backend.dtos.UserDto;
import org.springframework.stereotype.Service;

@Service

public class UserServiceImpl implements UserService{
    @Override
    public UserDto createUser(UserDto userDto) {
        return null;
    }

    @Override
    public UserDto getUserbyemail(String email) {
        return null;
    }

    @Override
    public UserDto updateuser(UserDto userDto, String userid) {
        return null;
    }

    @Override
    public void deleteuser(String userid) {

    }

    @Override
    public UserDto getuserbyid(String userid) {
        return null;
    }

    @Override
    public Iterable<UserDto> getallusers() {
        return null;
    }
}
