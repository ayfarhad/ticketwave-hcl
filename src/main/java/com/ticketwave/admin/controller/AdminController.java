package com.ticketwave.admin.controller;

import com.ticketwave.common.ApiResponse;
import com.ticketwave.user.User;
import com.ticketwave.user.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Administrative APIs")
public class AdminController {
    private final UserRepository userRepo;

    public AdminController(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    @GetMapping("/users")
    @Operation(summary = "Get all users")
    public ResponseEntity<ApiResponse<List<User>>> allUsers() {
        return ResponseEntity.ok(new ApiResponse<>(true, "ok", userRepo.findAll()));
    }

    @PostMapping("/users/{id}/disable")
    @Operation(summary = "Disable a user account")
    public ResponseEntity<ApiResponse<Void>> disable(@PathVariable Long id) {
        User u = userRepo.findById(id).orElseThrow();
        u.setEnabled(false);
        userRepo.save(u);
        return ResponseEntity.ok(new ApiResponse<>(true, "disabled", null));
    }
}
