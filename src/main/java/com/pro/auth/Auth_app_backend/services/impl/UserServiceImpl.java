package com.pro.auth.Auth_app_backend.services.impl;

import com.pro.auth.Auth_app_backend.dtos.UserDto;
import com.pro.auth.Auth_app_backend.entities.Provider;
import com.pro.auth.Auth_app_backend.entities.User;
import com.pro.auth.Auth_app_backend.exception.ResourceNotFoundException;
import com.pro.auth.Auth_app_backend.helpers.UserHelper;
import com.pro.auth.Auth_app_backend.repositories.UserRepository;
import com.pro.auth.Auth_app_backend.services.UserService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

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
    public UserDto getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        return modelMapper.map(user, UserDto.class);
    }

    @Override
    public UserDto updateuser(UserDto userDto, String userid) {
        UUID uId= UserHelper.parseUUID(userid);
        User existingUser = userRepository
                .findById(uId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with given id"));
//we are not going to change email id for this project.
        if (userDto.getName() != null) existingUser.setName(userDto.getName());
        if (userDto.getImage() != null) existingUser.setImage(userDto.getImage());
        if (userDto.getProvider() != null) existingUser.setProvider(userDto.getProvider());
//TODO: change password updation logic ...
        if (userDto.getPassword() != null) existingUser.setPassword(userDto.getPassword());
        existingUser.setEnabled(userDto.isEnabled());
        existingUser.setUpdatedAt(Instant.now());
        User updatedUser = userRepository.save(existingUser);
        return modelMapper.map(updatedUser, UserDto.class);

    }

    @Override
    public void deleteuser(String userid) {
        UUID uId= UserHelper.parseUUID(userid);
        User user = userRepository.findById(uId).orElseThrow(() -> new ResourceNotFoundException("User not found with given id"));
        userRepository.delete(user);
    }

    @Override
    public UserDto getUserById(String userid) {
        User user = userRepository.findById(UserHelper.parseUUID(userid)).orElseThrow(() -> new ResourceNotFoundException("User not found with given id"));
        return modelMapper.map(user,UserDto.class);
    }

    @Override
    @Transactional
    public Iterable<UserDto> getallusers() {
        return userRepository.findAll()
                .stream()
                .map(user -> modelMapper.map(user, UserDto.class))
                .toList();
    }
}
