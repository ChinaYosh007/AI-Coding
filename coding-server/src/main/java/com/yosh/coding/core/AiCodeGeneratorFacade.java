package com.yosh.coding.core;

import com.yosh.coding.artificalIntelligence.AiCodeGeneratorService;
import com.yosh.coding.artificalIntelligence.model.HtmlCodeResult;
import com.yosh.coding.artificalIntelligence.model.MultiFileCodeResult;
import com.yosh.coding.core.parser.CodeParserExcutor;
import com.yosh.coding.core.saver.CodeFilleSaveExecutor;
import com.yosh.exception.BusinessException;
import com.yosh.exception.ErrorCode;
import com.yosh.model.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;

/**
 * AI 代码生成外观类，组合生成和保存功能
 */
@Slf4j
@Service
public class AiCodeGeneratorFacade {

    @Resource
    private AiCodeGeneratorService aiCodeGeneratorService;
    private CodeParserExcutor codeParserExcutor;

    public Flux<String> processCodeStream(Flux<String> flux,CodeGenTypeEnum type){
        if(type == null){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型为空");
        }
        StringBuilder str = new StringBuilder();
        return flux.doOnNext(chunk -> str.append(chunk))
                .doOnComplete(()->{
                    try{
                        String content = str.toString();
                        Object exec = CodeParserExcutor.executeCode(content,type);
                        File file = CodeFilleSaveExecutor.saveFile(exec,type);
                        log.info("save file success:" + file.getName());
                    }catch (Exception e){
                        log.error("save file failed");
                    }
                });
    }
    /**
     * 统一入口：根据类型生成并保存代码
     *
     * @param userMessage     用户提示词
     * @param codeGenTypeEnum 生成类型
     * @return 保存的目录
     */
    public File generateAndSaveCode(String userMessage, CodeGenTypeEnum codeGenTypeEnum) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型为空");
        }
        return switch (codeGenTypeEnum) {
            case HTML -> {

                HtmlCodeResult file = aiCodeGeneratorService.generateHtmlCode(userMessage);
                yield CodeFilleSaveExecutor.saveFile(file,CodeGenTypeEnum.HTML);

            }
            case MULTI_FILE -> {
                MultiFileCodeResult file = aiCodeGeneratorService.generateMultiFileCode(userMessage);
                yield CodeFilleSaveExecutor.saveFile(file,CodeGenTypeEnum.MULTI_FILE);
            }
            default -> {
                String errorMessage = "不支持的生成类型：" + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }
    public Flux<String> generateAndSaveCodeStream(String userMessage, CodeGenTypeEnum codeGenTypeEnum) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型为空");
        }
        return switch (codeGenTypeEnum) {
            case HTML -> {
                Flux<String> flux = aiCodeGeneratorService.generateHtmlCodeStream(userMessage);
                yield processCodeStream(flux,CodeGenTypeEnum.HTML);
            }
            case MULTI_FILE -> {
                Flux<String> flux = aiCodeGeneratorService.generateMultiFileCodeStream(userMessage);
                yield processCodeStream(flux,CodeGenTypeEnum.MULTI_FILE);
            }
            default -> {
                String errorMessage = "不支持的生成类型：" + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }

}
