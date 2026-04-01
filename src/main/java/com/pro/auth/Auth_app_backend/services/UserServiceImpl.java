package com.pro.auth.Auth_app_backend.services;

import com.pro.auth.Auth_app_backend.dtos.UserDto;
import com.pro.auth.Auth_app_backend.entities.Provider;
import com.pro.auth.Auth_app_backend.entities.User;
import com.pro.auth.Auth_app_backend.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService{

    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    @Override
    public UserDto createUser(UserDto userDto) {
        if (userDto.getEmail()==null|| userDto.getEmail().isBlank()){
            throw  new IllegalArgumentException("Email is required");
        }
        if (userRepository.existsByEmail(userDto.getEmail())) {
            throw new IllegalArgumentException("email already exists");
        }
        // if you have extra checks __put here....
        User user = modelMapper.map(userDto, User.class);
        //role assign to new user for authorization......`
        user.setProvider(userDto.getProvider()!=null ? userDto.getProvider() : Provider.LOCAL);
         User savedUser = userRepository.save(user);

        return modelMapper.map(savedUser,UserDto.class);
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
        return userRepository.findAll()
                .stream()
                .map(user -> modelMapper.map(user, UserDto.class))
                .toList();
    }
}
