package com.wo.user.service.impl;

import com.wo.common.dto.LoginRequest;
import com.wo.common.dto.LoginResponse;
import com.wo.common.dto.UserCreateDTO;
import com.wo.common.enums.ErrorCode;
import com.wo.common.exception.BizException;
import com.wo.common.util.JwtUtil;
import com.wo.user.entity.SysUser;
import com.wo.user.mapper.UserMapper;
import com.wo.user.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public LoginResponse login(LoginRequest request) {
        // Find user by username
        SysUser user = userMapper.selectByUsername(request.getUsername());
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }

        // Check account status
        if (user.getStatus() == 0) {
            throw new BizException(ErrorCode.USER_DISABLED);
        }

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BizException(ErrorCode.INVALID_PASSWORD);
        }

        // Update last login time
        user.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(user);

        // Generate JWT token
        String token = JwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());

        // Build response
        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setRealName(user.getRealName());
        response.setRole(user.getRole());
        return response;
    }

    @Override
    public LoginResponse register(UserCreateDTO dto) {
        // Check if username already exists
        SysUser existingUser = userMapper.selectByUsername(dto.getUsername());
        if (existingUser != null) {
            throw new BizException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }

        // Create new user
        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRealName(dto.getRealName());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setDepartment(dto.getDepartment());
        user.setRole(dto.getRole() != null ? dto.getRole() : "USER");
        user.setStatus(1);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());

        // Save to database
        userMapper.insert(user);

        // Generate JWT token
        String token = JwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());

        // Build response
        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setRealName(user.getRealName());
        response.setRole(user.getRole());
        return response;
    }

    @Override
    public LoginResponse refreshToken(String token) {
        // Parse and validate old token
        Long userId = JwtUtil.getUserId(token);
        String username = JwtUtil.getUsername(token);
        String role = JwtUtil.getRole(token);

        if (userId == null || username == null) {
            throw new BizException(ErrorCode.INVALID_TOKEN);
        }

        // Verify user still exists and is active
        SysUser user = userMapper.selectById(userId);
        if (user == null || user.getStatus() == 0) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }

        // Generate new token
        String newToken = JwtUtil.generateToken(userId, username, role);

        // Build response
        LoginResponse response = new LoginResponse();
        response.setToken(newToken);
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setRealName(user.getRealName());
        response.setRole(user.getRole());
        return response;
    }
}
