package com.wo.api.dto.user;

import lombok.Data;

import java.io.Serializable;

@Data
public class LoginResponse implements Serializable {

    private String token;

    private Long userId;

    private String username;

    private String realName;

    private String role;
}
