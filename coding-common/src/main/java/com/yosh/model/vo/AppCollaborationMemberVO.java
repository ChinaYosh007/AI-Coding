package com.yosh.model.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class AppCollaborationMemberVO implements Serializable {

    /**
     * 用户 id
     */
    private Long userId;

    /**
     * 用户账号
     */
    private String userAccount;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 协作角色：owner / collaborator / viewer
     */
    private String role;

    private static final long serialVersionUID = 1L;
}