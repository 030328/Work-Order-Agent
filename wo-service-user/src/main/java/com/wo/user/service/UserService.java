package com.wo.user.service;

import com.wo.api.dto.user.UserCreateDTO;
import com.wo.api.dto.user.UserVO;
import com.wo.common.result.PageResult;

public interface UserService {

    /**
     * Get user by ID
     */
    UserVO getUserById(Long id);

    /**
     * Get user by username
     */
    UserVO getUserByUsername(String username);

    /**
     * List users with pagination and optional filters
     */
    PageResult<UserVO> listUsers(int page, int size, String role, String department);

    /**
     * Update user information
     */
    UserVO updateUser(Long id, UserCreateDTO dto);

    /**
     * Delete user by ID
     */
    void deleteUser(Long id);
}
