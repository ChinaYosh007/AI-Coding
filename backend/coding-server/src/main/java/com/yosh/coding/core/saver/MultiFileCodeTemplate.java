package com.yosh.coding.core.saver;

import cn.hutool.core.util.StrUtil;
import com.yosh.coding.artificalIntelligence.model.MultiFileCodeResult;
import com.yosh.exception.BusinessException;
import com.yosh.exception.ErrorCode;
import com.yosh.model.enums.CodeGenTypeEnum;

import java.util.ArrayList;
import java.util.List;

public class MultiFileCodeTemplate extends CodeSaverTemplate<MultiFileCodeResult> {
    @Override
    protected CodeGenTypeEnum getCodeType() {
        return CodeGenTypeEnum.MULTI_FILE;
    }

    @Override
    protected void valiedInput(MultiFileCodeResult res) {
        super.valiedInput(res);
        List<String> missingFiles = new ArrayList<>(3);
        if (StrUtil.isBlank(res.getHtmlCode())) {
            missingFiles.add("index.html");
        }
        if (StrUtil.isBlank(res.getCssCode())) {
            missingFiles.add("style.css");
        }
        if (StrUtil.isBlank(res.getJsCode())) {
            missingFiles.add("script.js");
        }
        if (!missingFiles.isEmpty()) {
            throw new BusinessException(
                    ErrorCode.SYSTEM_ERROR,
                    "AI 返回的多文件代码不完整，缺少 " + String.join("、", missingFiles) + "，请重试");
        }
    }

    @Override
    protected void saveFile(MultiFileCodeResult res, String dir) {
        writeFile(dir,"index.html",res.getHtmlCode());
        writeFile(dir,"style.css",res.getCssCode());
        writeFile(dir,"script.js",res.getJsCode());
    }
}
