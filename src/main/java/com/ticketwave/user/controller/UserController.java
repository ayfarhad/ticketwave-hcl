package com.ticketwave.user.controller;

import com.ticketwave.common.ApiResponse;
import com.ticketwave.user.User;
import com.ticketwave.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/api/users/by-username")
    @PreAuthorize("hasAnyRole('ADMIN','SUPPORT')")
    public ResponseEntity<ApiResponse<User>> getByUsername(@RequestParam String username) {
        return ResponseEntity.ok(new ApiResponse<>(true, "ok", userService.getByUsername(username)));
    }
}
