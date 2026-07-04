package com.yosh.model.dto.app;

import lombok.Data;

import java.io.Serializable;

@Data
public class AppCollaborationInviteRequest implements Serializable {

    /**
     * 应用 id
     */
    private Long appId;

    /**
     * 被邀请用户账号
     */
    private String userAccount;

    private static final long serialVersionUID = 1L;
}