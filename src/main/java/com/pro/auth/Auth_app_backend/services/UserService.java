package com.pro.auth.Auth_app_backend.services;

import com.pro.auth.Auth_app_backend.dtos.UserDto;

public interface UserService {
    // create user
    UserDto createUser(UserDto userDto);
    //get user by email
    UserDto getUserByEmail(String email);
    //updateuser
    UserDto updateuser(UserDto userDto,String userid);
    //deleteuser
    void deleteuser(String userid);
    //get user by id
    UserDto getUserById(String userid);
    //getalluser
    Iterable<UserDto> getallusers();

}
