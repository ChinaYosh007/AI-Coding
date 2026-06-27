package com.yosh.coding.core;

import com.yosh.coding.artificalIntelligence.AiCodeGeneratorService;
import com.yosh.coding.artificalIntelligence.model.HtmlCodeResult;
import com.yosh.coding.artificalIntelligence.model.MultiFileCodeResult;
import com.yosh.exception.BusinessException;
import com.yosh.exception.ErrorCode;
import com.yosh.model.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;

/**
 * AI 代码生成外观类，组合生成和保存功能
 */
@Service
public class AiCodeGeneratorFacade {

    @Resource
    private AiCodeGeneratorService aiCodeGeneratorService;

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
            case HTML -> generateAndSaveHtmlCode(userMessage);
            case MULTI_FILE -> generateAndSaveMultiFileCode(userMessage);
            default -> {
                String errorMessage = "不支持的生成类型：" + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }

    /**
     * 生成 HTML 模式的代码并保存
     *
     * @param userMessage 用户提示词
     * @return 保存的目录
     */
    private File generateAndSaveHtmlCode(String userMessage) {
        HtmlCodeResult result = aiCodeGeneratorService.generateHtmlCode(userMessage);
        return OutputFileCore.saveHTMLCode(result);
    }



    /**
     * 生成多文件模式的代码并保存
     *
     * @param userMessage 用户提示词
     * @return 保存的目录
     */
    private File generateAndSaveMultiFileCode(String userMessage) {
        MultiFileCodeResult result = aiCodeGeneratorService.generateMultiFileCode(userMessage);
        return OutputFileCore.saveMultiFileCodeResult(result);
    }

    public Flux<String> generateAndSaveCodeStream(String userMessage, CodeGenTypeEnum codeGenTypeEnum) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型为空");
        }
        return switch (codeGenTypeEnum) {
            case HTML -> generateAndSaveHtmlCodeStream(userMessage);
            case MULTI_FILE -> generateAndSaveMultiFileCodeStream(userMessage);
            default -> {
                String errorMessage = "不支持的生成类型：" + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }

    public Flux<String> generateAndSaveHtmlCodeStream(String userMessage) {
     Flux<String> res = aiCodeGeneratorService.generateHtmlCodeStream(userMessage);
     StringBuilder stringBuilder = new StringBuilder();
     return res
             .doOnNext(chunk ->{
                 stringBuilder.append(chunk);
             })
             .doOnComplete(()->{
                 try{
                     String str = stringBuilder.toString();
                     HtmlCodeResult html = CodeParser.parseHtmlCode(str);
                     System.out.println(OutputFileCore.saveHTMLCode(html));
                 } catch (RuntimeException e) {
                     throw new RuntimeException(e);
                 }
             });
    }

    /**
     * 生成 HTML 模式的代码并保存
     *
     * @param userMessage 用户提示词
     * @return 保存的目录
     */
    private Flux<String> generateAndSaveMultiFileCodeStream(String userMessage) {
      Flux<String> res = aiCodeGeneratorService.generateMultiFileCodeStream(userMessage);
      StringBuilder stringBuilder = new StringBuilder();
      return  res
              .doOnNext(chunk ->{
          stringBuilder.append(chunk);
      })
              .doOnComplete(()->{
                  try {
                      String str = stringBuilder.toString();
                      MultiFileCodeResult mutil = CodeParser.parseMultiFileCode(str);
                      System.out.println(OutputFileCore.saveMultiFileCodeResult(mutil));

                  }
                  catch (Exception e){
                      e.printStackTrace();
                  }

              });
    }

}
