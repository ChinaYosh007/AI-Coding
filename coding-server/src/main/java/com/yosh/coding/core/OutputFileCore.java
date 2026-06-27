package com.yosh.coding.core;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.yosh.coding.artificalIntelligence.model.HtmlCodeResult;
import com.yosh.coding.artificalIntelligence.model.MultiFileCodeResult;
import com.yosh.model.enums.CodeGenTypeEnum;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public class OutputFileCore {
    private static final String URL = System.getProperty("user.dir") + "/src/main/resources/static/tmp/output_file";
    private static String builderPath(String prefix){
        String fileName = StrUtil.format("{}_{}",prefix, IdUtil.getSnowflakeNextIdStr());
        String dir = URL + File.separator + fileName;
        FileUtil.mkdir(dir);
        return dir;
    }

    private static  void writeFile(String dir,String fileName,String resource) {
        String file = dir + File.separator + fileName;
        FileUtil.writeString(resource,file, StandardCharsets.UTF_8);


    }

    public static File saveHTMLCode(HtmlCodeResult resource){
        String dir = builderPath(CodeGenTypeEnum.HTML.getValue());
        writeFile(dir,"index.html",resource.getHtmlCode());
        return new File(dir);
    }
    public static File saveMultiFileCodeResult(MultiFileCodeResult resource){
        String dir = builderPath(CodeGenTypeEnum.MULTI_FILE.getValue());
        writeFile(dir,"index.html",resource.getHtmlCode());
        writeFile(dir,"style.css",resource.getCssCode());
        writeFile(dir,"script.js",resource.getJsCode());
        return new File(dir);
    }

}
