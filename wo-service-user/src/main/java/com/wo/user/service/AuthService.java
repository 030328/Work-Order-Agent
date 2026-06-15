package com.wo.user.service;

import com.wo.common.dto.LoginRequest;
import com.wo.common.dto.LoginResponse;
import com.wo.common.dto.UserCreateDTO;

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
