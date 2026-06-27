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
            生成一个青春少女系列的网站，女主是jk系列
            """;
    @Test
    public void chatWithSaver(){
        File file = aiCodeGeneratorFacade.generateAndSaveCode(message, CodeGenTypeEnum.MULTI_FILE);
        Assertions.assertNotNull(file);
    }
    @Test
    public void chatWithSaverStream(){
        Flux<String> file = aiCodeGeneratorFacade.generateAndSaveCodeStream(message, CodeGenTypeEnum.HTML);
        file.doOnNext(System.out::print)
                .doOnComplete(()-> System.out.println("success")).blockLast();


    }
}
