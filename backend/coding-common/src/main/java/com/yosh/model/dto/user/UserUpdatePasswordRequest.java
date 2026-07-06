package com.yosh.model.dto.user;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserUpdatePasswordRequest implements Serializable {

    /**
     * Current password.
     */
    private String oldPassword;

    /**
     * New password.
     */
    private String newPassword;

    /**
     * Confirm password.
     */
    private String checkPassword;

    private static final long serialVersionUID = 1L;
}