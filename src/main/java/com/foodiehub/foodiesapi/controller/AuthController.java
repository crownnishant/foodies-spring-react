package com.foodiehub.foodiesapi.controller;

import com.foodiehub.foodiesapi.io.AuthenticationRequest;
import com.foodiehub.foodiesapi.io.AuthenticationResponse;
import com.foodiehub.foodiesapi.service.impl.AppUserDetailsService;
import com.foodiehub.foodiesapi.util.JwtUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@CrossOrigin("*")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final AppUserDetailsService appUserDetailsService;
    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    public AuthenticationResponse login(@RequestBody AuthenticationRequest request){
        System.out.println("Login request received for email: " + request.getEmail());
        authenticationManager.authenticate((new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())));
        final UserDetails userDetails= appUserDetailsService.loadUserByUsername(request.getEmail());
        final String jwtToken=jwtUtil.generateToken(userDetails);
        return new AuthenticationResponse(request.getEmail(), jwtToken);
    }

}
