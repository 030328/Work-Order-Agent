package com.wo.user.controller;

import com.wo.common.dto.LoginRequest;
import com.wo.common.dto.LoginResponse;
import com.wo.common.dto.UserCreateDTO;
import com.wo.common.result.R;
import com.wo.user.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * User login
     */
    @PostMapping("/login")
    public R<LoginResponse> login(@RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return R.ok(response);
    }

    /**
     * User registration
     */
    @PostMapping("/register")
    public R<LoginResponse> register(@RequestBody UserCreateDTO dto) {
        LoginResponse response = authService.register(dto);
        return R.ok(response);
    }

    /**
     * Refresh JWT token
     */
    @PostMapping("/refresh")
    public R<LoginResponse> refreshToken(@RequestBody String token) {
        LoginResponse response = authService.refreshToken(token);
        return R.ok(response);
    }
}
