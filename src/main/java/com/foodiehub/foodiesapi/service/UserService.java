package com.foodiehub.foodiesapi.service;

import com.foodiehub.foodiesapi.io.UserRequest;
import com.foodiehub.foodiesapi.io.UserResponse;

public interface UserService {
    UserResponse registerUser(UserRequest userRequest);

//get logged in User details in Cart service
    Long findByUserId();
}
