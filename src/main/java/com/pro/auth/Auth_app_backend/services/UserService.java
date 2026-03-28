package com.pro.auth.Auth_app_backend.services;

import com.pro.auth.Auth_app_backend.dtos.UserDto;

public interface UserService {
    // create user
    UserDto createUser(UserDto userDto);
    //get user by email
    UserDto getUserbyemail(String email);
    //updateuser
    UserDto updateuser(UserDto userDto,String userid);
    //deleteuser
    void deleteuser(String userid);
    //get user by id
    UserDto getuserbyid(String userid);
    //getalluser
    Iterable<UserDto> getallusers();

}
