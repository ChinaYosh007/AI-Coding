package com.yosh.coding.core;

import com.yosh.model.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.CountDownLatch;

@SpringBootTest
public class AiCodeGeneratorFacadeTest {
    @Resource
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;
    public static final String message = """
            生成奥特曼大全的网站，页面要牛逼
            """;
    @Test
    public void chatWithSaver(){
        Long appId = 1L;
        File file = aiCodeGeneratorFacade.generateAndSaveCode(message, CodeGenTypeEnum.MULTI_FILE,appId,1L);
        Assertions.assertNotNull(file);
    }
    @Test
    public void chatWithSaverStream(){
        Long appId = 1L;
        Flux<String> stream = aiCodeGeneratorFacade.generateAndSaveCodeStream("test message", CodeGenTypeEnum.VUE_PROJECT, 1L, 1L, false);
        stream.doOnNext(System.out::print)
                .doOnComplete(()-> System.out.println("success")).blockLast();


    }
}
