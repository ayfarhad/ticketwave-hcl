package com.ticketwave.auth;

import com.ticketwave.auth.dto.LoginRequest;
import com.ticketwave.auth.dto.RegisterRequest;
import com.ticketwave.auth.service.AuthServiceImpl;
import com.ticketwave.common.ApiResponse;
import com.ticketwave.user.User;
import com.ticketwave.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceImplTest {
    @Mock
    private UserRepository userRepo;
    @Mock
    private PasswordEncoder encoder;
    @Mock
    private JwtUtils jwtUtils;
    @InjectMocks
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void loginSuccess() {
        User u = new User();
        u.setUsername("john");
        u.setPassword("hashed");
        u.setRole("CUSTOMER");
        when(userRepo.findByUsername("john")).thenReturn(Optional.of(u));
        when(encoder.matches("pwd","hashed")).thenReturn(true);
        when(jwtUtils.generate(anyMap(), eq("john"))).thenReturn("token");

        LoginRequest req = new LoginRequest();
        req.setUsername("john");
        req.setPassword("pwd");
        ApiResponse<String> resp = authService.login(req);
        assertTrue(resp.isSuccess());
        assertEquals("token", resp.getData());
    }

    @Test
    void registerDuplicateUsername() {
        when(userRepo.findByUsername("john")).thenReturn(Optional.of(new User()));
        RegisterRequest req = new RegisterRequest();
        req.setUsername("john");
        req.setEmail("e@test.com");
        req.setPassword("pwd");
        ApiResponse<Void> resp = authService.register(req);
        assertFalse(resp.isSuccess());
    }
}
