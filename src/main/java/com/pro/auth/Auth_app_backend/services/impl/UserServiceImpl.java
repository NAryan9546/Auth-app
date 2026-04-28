package com.pro.auth.Auth_app_backend.services.impl;

import com.pro.auth.Auth_app_backend.dtos.UserDto;
import com.pro.auth.Auth_app_backend.entities.Provider;
import com.pro.auth.Auth_app_backend.entities.User;
import com.pro.auth.Auth_app_backend.exception.EmailAlreadyExistsException;
import com.pro.auth.Auth_app_backend.exception.ResourceNotFoundException;
import com.pro.auth.Auth_app_backend.helpers.UserHelper;
import com.pro.auth.Auth_app_backend.repositories.UserRepository;
import com.pro.auth.Auth_app_backend.services.UserService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    @Override
    public UserDto createUser(UserDto userDto) {
        // 1. Basic Validation
        if (userDto.getEmail() == null || userDto.getEmail().isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }

        // 2. Check if email exists to trigger the 401 Unauthorized error
        if (userRepository.existsByEmail(userDto.getEmail())) {
            throw new EmailAlreadyExistsException("email already exists");
        }

        // 3. Map DTO to Entity
        User user = modelMapper.map(userDto, User.class);

        // 4. Handle default provider logic
        // If provider is null in request, default to LOCAL
        // Change this line
        if (userDto.getProvider() == null) {
            user.setProvider(Provider.LOCAL); // Use the Enum constant, no quotes
        }

        // 5. Save and Return
        User savedUser = userRepository.save(user);
        return modelMapper.map(savedUser, UserDto.class);
    }

    @Override
    public UserDto getUserById(String userid) {
        UUID uId = UserHelper.parseUUID(userid);
        User user = userRepository.findById(uId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userid));
        return modelMapper.map(user, UserDto.class);
    }

    @Override
    public UserDto getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        return modelMapper.map(user, UserDto.class);
    }

    @Override
    @Transactional
    public Iterable<UserDto> getallusers() {
        List<User> users = userRepository.findAll();
        return users.stream()
                .map(user -> modelMapper.map(user, UserDto.class))
                .collect(Collectors.toList());
    }

    @Override
    public UserDto updateuser(UserDto userDto, String userid) {
        UUID uId = UserHelper.parseUUID(userid);
        User existingUser = userRepository.findById(uId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userid));

        // Update fields
        existingUser.setName(userDto.getName());
        existingUser.setImage(userDto.getImage());
        existingUser.setEnabled(userDto.isEnabled());

        User updatedUser = userRepository.save(existingUser);
        return modelMapper.map(updatedUser, UserDto.class);
    }

    @Override
    public void deleteuser(String userid) {
        UUID uId = UserHelper.parseUUID(userid);
        User user = userRepository.findById(uId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userid));
        userRepository.delete(user);
    }
}