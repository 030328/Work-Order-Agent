package com.wo.user.service;

import com.wo.api.dto.user.LoginRequest;
import com.wo.api.dto.user.LoginResponse;
import com.wo.api.dto.user.UserCreateDTO;

public interface AuthService {

    /**
     * User login with username and password
     */
    LoginResponse login(LoginRequest request);

    /**
     * Register a new user account
     */
    LoginResponse register(UserCreateDTO dto);

    /**
     * Refresh an existing JWT token
     */
    LoginResponse refreshToken(String token);
}
