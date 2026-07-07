package com.wo.user.controller;

import com.wo.api.dto.user.UserCreateDTO;
import com.wo.api.dto.user.UserVO;
import com.wo.common.result.PageResult;
import com.wo.common.result.R;
import com.wo.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Get user by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','SYSTEM') or #id.toString() == authentication.name")
    public R<UserVO> getUserById(@PathVariable Long id) {
        UserVO user = userService.getUserById(id);
        return R.ok(user);
    }

    @GetMapping("/by-username/{username}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','SYSTEM')")
    public R<UserVO> getUserByUsername(@PathVariable String username) {
        UserVO user = userService.getUserByUsername(username);
        return R.ok(user);
    }

    /**
     * List users with pagination and optional filters
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','SYSTEM')")
    public R<PageResult<UserVO>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String department) {
        PageResult<UserVO> result = userService.listUsers(page, size, role, department);
        return R.ok(result);
    }

    /**
     * Update user information
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public R<UserVO> updateUser(@PathVariable Long id, @RequestBody UserCreateDTO dto) {
        UserVO user = userService.updateUser(id, dto);
        return R.ok(user);
    }

    /**
     * Delete user by ID
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return R.ok();
    }
}
