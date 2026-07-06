package com.yosh.model.dto.user;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserUpdateMyRequest implements Serializable {

    /**
     * User nickname.
     */
    private String userName;

    /**
     * User avatar URL.
     */
    private String userAvatar;

    /**
     * User profile.
     */
    private String userProfile;

    private static final long serialVersionUID = 1L;
}