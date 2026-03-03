package com.ticketwave.user.service;

import com.ticketwave.user.User;
import com.ticketwave.user.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepo;

    public UserServiceImpl(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    @Override
    public User getByUsername(String username) {
        return userRepo.findByUsername(username).orElseThrow();
    }
}
