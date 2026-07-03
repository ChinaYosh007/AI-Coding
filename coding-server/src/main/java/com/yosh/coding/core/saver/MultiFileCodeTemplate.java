package com.yosh.coding.core.saver;

import com.yosh.coding.artificalIntelligence.model.MultiFileCodeResult;
import com.yosh.model.enums.CodeGenTypeEnum;

public class MultiFileCodeTemplate extends CodeSaverTemplate<MultiFileCodeResult> {
    @Override
    protected CodeGenTypeEnum getCodeType() {
        return CodeGenTypeEnum.MULTI_FILE;
    }

    @Override
    protected void saveFile(MultiFileCodeResult res, String dir) {
        writeFile(dir,"index.html",res.getHtmlCode());
        writeFile(dir,"style.css",res.getCssCode());
        writeFile(dir,"script.js",res.getJsCode());
    }
}
