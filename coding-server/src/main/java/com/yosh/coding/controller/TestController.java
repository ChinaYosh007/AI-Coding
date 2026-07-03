package com.yosh.coding.controller;

import com.yosh.common.BaseResponse;
import com.yosh.common.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("")
@Slf4j
public class TestController {
    private String URL = "http://localhost:8080/api/swagger-ui/index.html";
    @GetMapping("/health")
    public BaseResponse<String> health()
    {

        log.info("Health check");
        return ResultUtils.success("OK");
    }
}
