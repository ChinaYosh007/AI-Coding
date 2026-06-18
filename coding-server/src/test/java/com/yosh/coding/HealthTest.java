package com.yosh.coding;

import com.yosh.coding.controller.TestController;
import com.yosh.common.BaseResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class HealthTest {

    @Autowired
    private TestController testController;

    @Test
    void healthCheck() {
        BaseResponse<String> response = testController.health();
        Assertions.assertEquals(0, response.getCode());
    }
}
