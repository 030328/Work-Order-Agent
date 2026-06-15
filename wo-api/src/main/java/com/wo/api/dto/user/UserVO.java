package com.wo.api.dto.user;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class UserVO implements Serializable {

    private Long id;

    private String username;

    private String realName;

    private String email;

    private String phone;

    private String avatar;

    private String department;

    private String role;

    private String status;

    private LocalDateTime lastLoginTime;

    private LocalDateTime createdAt;
}
