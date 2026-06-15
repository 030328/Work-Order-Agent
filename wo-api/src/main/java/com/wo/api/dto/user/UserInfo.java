package com.wo.api.dto.user;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserInfo implements Serializable {

    private Long id;

    private String username;

    private String realName;

    private String role;

    private String department;
}
