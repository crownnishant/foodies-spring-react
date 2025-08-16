package com.foodiehub.foodiesapi.service.impl;

import com.foodiehub.foodiesapi.entities.UserEntity;
import com.foodiehub.foodiesapi.io.UserRequest;
import com.foodiehub.foodiesapi.io.UserResponse;
import com.foodiehub.foodiesapi.repository.UserRepository;
import com.foodiehub.foodiesapi.service.AuthenticationFacade;
import com.foodiehub.foodiesapi.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationFacade authenticationFacade;

    @Override
    public UserResponse registerUser(UserRequest userRequest) {
        UserEntity newUser = convertToEntity(userRequest);
        newUser = userRepository.save(newUser);
        return convertToResponse(newUser);
    }

    @Override
    public Long findByUserId() {
        String loggedInUserEmail = authenticationFacade.getAuthentication().getName();
        UserEntity loggedInUser=userRepository.findByEmail(loggedInUserEmail)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + loggedInUserEmail));
        return loggedInUser.getId();
    }

    //Once we get userrequest, we need to convert it into Entity.
//copy all the values from UserRequest to UserEntity
    private UserEntity convertToEntity(UserRequest userRequest){
        return UserEntity.builder()
                .email(userRequest.getEmail())
                .password(passwordEncoder.encode(userRequest.getPassword()))
                .name(userRequest.getName())
                .build();
    }

    private UserResponse convertToResponse(UserEntity registeredUser){
        return UserResponse.builder()
                .id(registeredUser.getId())
                .name(registeredUser.getName())
                .email(registeredUser.getEmail())
                .build();
    }
}
