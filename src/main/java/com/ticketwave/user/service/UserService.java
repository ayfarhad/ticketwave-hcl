package com.ticketwave.user.service;

import com.ticketwave.user.User;

public interface UserService {
    User getByUsername(String username);
}
