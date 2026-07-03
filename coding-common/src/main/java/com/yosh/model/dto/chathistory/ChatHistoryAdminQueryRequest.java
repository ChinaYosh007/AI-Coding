package com.yosh.model.dto.chathistory;

import com.yosh.common.PageResquest;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
public class ChatHistoryAdminQueryRequest extends PageResquest implements Serializable {

    private Long appId;
    private Long userId;
    private String messageType;

    private static final long serialVersionUID = 1L;
}