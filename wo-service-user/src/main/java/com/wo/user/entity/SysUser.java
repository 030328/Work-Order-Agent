package com.wo.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wo.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {

    /**
     * Username / login name
     */
    private String username;

    /**
     * Encrypted password
     */
    private String password;

    /**
     * Real name
     */
    private String realName;

    /**
     * Email address
     */
    private String email;

    /**
     * Phone number
     */
    private String phone;

    /**
     * Avatar URL
     */
    private String avatar;

    /**
     * Department name
     */
    private String department;

    /**
     * User role (default USER)
     */
    private String role = "USER";

    /**
     * Account status: 1=active, 0=disabled
     */
    private Integer status = 1;

    /**
     * Last login timestamp
     */
    private LocalDateTime lastLoginTime;
}
