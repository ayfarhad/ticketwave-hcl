package com.ticketwave.auth.service;

import com.ticketwave.auth.dto.LoginRequest;
import com.ticketwave.auth.dto.RegisterRequest;
import com.ticketwave.common.ApiResponse;

public interface AuthService {
    ApiResponse<String> login(LoginRequest request);
    ApiResponse<Void> register(RegisterRequest request);
}
