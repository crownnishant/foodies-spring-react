package com.foodiehub.foodiesapi.controller;

import com.foodiehub.foodiesapi.io.UserRequest;
import com.foodiehub.foodiesapi.io.UserResponse;
import com.foodiehub.foodiesapi.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
@CrossOrigin("*")
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse registerUser(@RequestBody UserRequest userRequest){
        return userService.registerUser(userRequest);

    }
}
