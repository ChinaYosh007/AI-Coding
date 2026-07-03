package com.yosh.coding.core.saver;

import com.yosh.coding.artificalIntelligence.model.HtmlCodeResult;
import com.yosh.model.enums.CodeGenTypeEnum;

public class HtmlCodeTemplate extends CodeSaverTemplate<HtmlCodeResult> {
    @Override
    protected CodeGenTypeEnum getCodeType() {
        return CodeGenTypeEnum.HTML;
    }

    @Override
    protected void saveFile(HtmlCodeResult res, String dir) {
        writeFile(dir,"index.html", res.getHtmlCode());
    }
    @Override
    protected void valiedInput(HtmlCodeResult htmlCodeResult){
        super.valiedInput(htmlCodeResult);
    }
}
