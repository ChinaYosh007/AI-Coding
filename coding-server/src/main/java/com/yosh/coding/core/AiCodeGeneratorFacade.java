package com.yosh.coding.core;

import com.yosh.coding.artificalIntelligence.AiCodeGeneratorService;
import com.yosh.coding.artificalIntelligence.model.HtmlCodeResult;
import com.yosh.coding.artificalIntelligence.model.MultiFileCodeResult;
import com.yosh.coding.core.parser.CodeParserExcutor;
import com.yosh.coding.core.saver.CodeFilleSaveExecutor;
import com.yosh.exception.BusinessException;
import com.yosh.exception.ErrorCode;
import com.yosh.model.costants.AppConstant;
import com.yosh.model.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SignalType;

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

    public Flux<String> processCodeStream(Flux<String> flux,CodeGenTypeEnum type,Long appId,Long version){
        if(type == null){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型为空");
        }
        StringBuilder str = new StringBuilder();
        return flux.doOnNext(chunk -> str.append(chunk))
                .doFinally(signalType -> {
                    // 无论流是完成、错误还是取消，都执行保存
                    if (!signalType.equals(SignalType.CANCEL)) {
                        try{
                            String content = str.toString();
                            log.info("Code content length: {}", content.length());
                            Object exec = CodeParserExcutor.executeCode(content,type);
                            log.info("Code parsed successfully: {}", exec.getClass().getSimpleName());
                            File file = CodeFilleSaveExecutor.saveFile(exec,type,appId,version);
                            log.info("save file success: {}", file.getAbsolutePath());
                        }catch (Exception e){
                            log.error("save file failed: {}", e.getMessage(), e);
                            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Failed to save code: " + e.getMessage());
                        }
                    } else {
                        log.warn("Stream cancelled, skipping save");
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
    public File generateAndSaveCode(String userMessage, CodeGenTypeEnum codeGenTypeEnum,Long appId,Long version) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型为空");
        }
        return switch (codeGenTypeEnum) {
            case HTML -> {

                HtmlCodeResult file = aiCodeGeneratorService.generateHtmlCode(appId,userMessage);
                yield CodeFilleSaveExecutor.saveFile(file,CodeGenTypeEnum.HTML,appId, version);

            }
            case MULTI_FILE -> {
                MultiFileCodeResult file = aiCodeGeneratorService.generateMultiFileCode(appId,userMessage);
                yield CodeFilleSaveExecutor.saveFile(file,CodeGenTypeEnum.MULTI_FILE,appId,version);
            }
            default -> {
                String errorMessage = "不支持的生成类型：" + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }
    public Flux<String> generateAndSaveCodeStream(String userMessage, CodeGenTypeEnum codeGenTypeEnum,Long appId,Long version) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型为空");
        }
        return switch (codeGenTypeEnum) {
            case HTML -> {
                Flux<String> flux = aiCodeGeneratorService.generateHtmlCodeStream(appId,userMessage);
                yield processCodeStream(flux,CodeGenTypeEnum.HTML,appId,version);
            }
            case MULTI_FILE -> {
                Flux<String> flux = aiCodeGeneratorService.generateMultiFileCodeStream(appId,userMessage);
                yield processCodeStream(flux,CodeGenTypeEnum.MULTI_FILE,appId,version);
            }
            default -> {
                String errorMessage = "不支持的生成类型：" + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }

    public String generateAppName(String initPrompt) {
        return aiCodeGeneratorService.generateAppName(initPrompt);
    }

    public String summarizeAppChatHistoryMemory(String markdown, Long appId) {
        return aiCodeGeneratorService.summarizeAppChatHistoryMemory(markdown, appId);
    }

    public String getAppChatHistoryMemory(Long appId) {
        return aiCodeGeneratorService.getAppChatHistoryMemory(appId);
    }
}
