package com.yosh.coding.core.saver;

import com.yosh.coding.artificalIntelligence.model.HtmlCodeResult;
import com.yosh.coding.artificalIntelligence.model.MultiFileCodeResult;
import com.yosh.exception.BusinessException;
import com.yosh.exception.ErrorCode;
import com.yosh.model.enums.CodeGenTypeEnum;

import java.io.File;
import java.util.List;

public class CodeFilleSaveExecutor {
    public static final HtmlCodeTemplate htmlCodeTemplate = new HtmlCodeTemplate();
    public static final MultiFileCodeTemplate multiFileCodeTemplate = new MultiFileCodeTemplate();

    public static File saveFile(Object res, CodeGenTypeEnum type, Long appId, Long version) {
        return saveFile(res, type, appId, version, List.of());
    }

    public static File saveFile(Object res, CodeGenTypeEnum type, Long appId, Long version, List<String> resourceUrls) {
        return PlaceholderImageUrlSanitizer.withReplacementUrls(resourceUrls, () -> switch (type) {
            case HTML -> htmlCodeTemplate.saveCode((HtmlCodeResult) res, appId, version);
            case MULTI_FILE -> multiFileCodeTemplate.saveCode((MultiFileCodeResult) res, appId, version);
            default -> throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Unsupported code generation type");
        });
    }
}
