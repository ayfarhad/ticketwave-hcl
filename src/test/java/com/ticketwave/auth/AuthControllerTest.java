package com.ticketwave.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketwave.auth.dto.LoginRequest;
import com.ticketwave.auth.dto.RegisterRequest;
import com.ticketwave.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AuthControllerTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private UserRepository userRepo;
    @Autowired
    private ObjectMapper mapper;

    @Test
    void registerAndLogin() throws Exception {
        RegisterRequest reg = new RegisterRequest();
        reg.setUsername("alice");
        reg.setEmail("alice@test.com");
        reg.setPassword("pwd");
        mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(reg)))
                .andExpect(status().isOk());

        LoginRequest l = new LoginRequest();
        l.setUsername("alice");
        l.setPassword("pwd");
        mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(l)))
                .andExpect(status().isOk());
    }
}
