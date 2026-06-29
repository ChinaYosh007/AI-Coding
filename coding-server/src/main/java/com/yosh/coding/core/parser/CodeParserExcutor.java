package com.yosh.coding.core.parser;

import com.yosh.exception.BusinessException;
import com.yosh.exception.ErrorCode;
import com.yosh.model.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;

public class CodeParserExcutor {

    private static final HtmlCodePaser htmlCodePaser = new HtmlCodePaser();

    private static final MultiFIleCodeParser multiFIleCodeParser = new MultiFIleCodeParser();

    public static  Object executeCode(String content, CodeGenTypeEnum type){
        return switch (type){
            case HTML -> htmlCodePaser.parseCode(content);
            case MULTI_FILE -> multiFIleCodeParser.parseCode(content);
            default -> throw new BusinessException(ErrorCode.SYSTEM_ERROR,"不支持该文件");
        };
    }


}
