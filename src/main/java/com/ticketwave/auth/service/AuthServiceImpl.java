package com.ticketwave.auth.service;

import com.ticketwave.auth.JwtUtils;
import com.ticketwave.auth.dto.LoginRequest;
import com.ticketwave.auth.dto.RegisterRequest;
import com.ticketwave.common.ApiResponse;
import com.ticketwave.user.User;
import com.ticketwave.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityNotFoundException;
import java.util.HashMap;
import java.util.Map;

@Service
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepo;
    private final PasswordEncoder encoder;
    private final JwtUtils jwtUtils;

    public AuthServiceImpl(UserRepository userRepo, PasswordEncoder encoder, JwtUtils jwtUtils) {
        this.userRepo = userRepo;
        this.encoder = encoder;
        this.jwtUtils = jwtUtils;
    }

    @Override
    public ApiResponse<String> login(LoginRequest request) {
        User user = userRepo.findByUsername(request.getUsername())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        if (!encoder.matches(request.getPassword(), user.getPassword())) {
            return new ApiResponse<>(false, "Invalid credentials", null);
        }
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", user.getRole());
        String token = jwtUtils.generate(claims, user.getUsername());
        return new ApiResponse<>(true, "Success", token);
    }

    @Override
    public ApiResponse<Void> register(RegisterRequest request) {
        if (userRepo.findByUsername(request.getUsername()).isPresent()) {
            return new ApiResponse<>(false, "Username taken", null);
        }
        if (userRepo.findByEmail(request.getEmail()).isPresent()) {
            return new ApiResponse<>(false, "Email taken", null);
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(encoder.encode(request.getPassword()));
        user.setRole("CUSTOMER");
        userRepo.save(user);
        return new ApiResponse<>(true, "Registered", null);
    }
}
