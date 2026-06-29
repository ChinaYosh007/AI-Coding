package com.yosh.coding.core.saver;

import com.yosh.coding.artificalIntelligence.model.HtmlCodeResult;
import com.yosh.coding.artificalIntelligence.model.MultiFileCodeResult;
import com.yosh.exception.BusinessException;
import com.yosh.exception.ErrorCode;
import com.yosh.exception.ThrowUtils;
import com.yosh.model.enums.CodeGenTypeEnum;

import java.io.File;

public class CodeFilleSaveExecutor {
    public static final HtmlCodeTemplate htmlCodeTemplate = new HtmlCodeTemplate();
    public static final MultiFileCodeTemplate multiFileCodeTemplate = new MultiFileCodeTemplate();

    public static final File saveFile(Object res, CodeGenTypeEnum type){
        return switch (type){
            case HTML ->  htmlCodeTemplate.saveCode((HtmlCodeResult) res);
            case MULTI_FILE -> multiFileCodeTemplate.saveCode((MultiFileCodeResult) res);
            default -> throw  new BusinessException(ErrorCode.SYSTEM_ERROR,"暂不支持该文件");
        };
    }
}