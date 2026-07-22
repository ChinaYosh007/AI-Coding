package com.yosh.model.dto.app;

import lombok.Data;
import java.io.Serializable;

@Data
public class AppSaveFileRequest implements Serializable {
    private String filePath;
    private String content;
    private static final long serialVersionUID = 1L;
}
