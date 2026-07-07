package com.wo.user.controller;

import com.wo.api.dto.user.LoginRequest;
import com.wo.api.dto.user.LoginResponse;
import com.wo.api.dto.user.UserCreateDTO;
import com.wo.common.result.R;
import com.wo.user.service.AuthService;
import jakarta.validation.Valid;
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
    public R<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return R.ok(response);
    }

    /**
     * User registration
     */
    @PostMapping("/register")
    public R<LoginResponse> register(@Valid @RequestBody UserCreateDTO dto) {
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
