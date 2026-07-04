package com.yosh.coding.exception;


import com.yosh.common.BaseResponse;
import com.yosh.common.ResultUtils;
import com.yosh.exception.BusinessException;
import com.yosh.exception.ErrorCode;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Hidden
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public BaseResponse<?> handler(BusinessException e) {
        if (e.getCode() == ErrorCode.NOT_LOGIN_ERROR.getCode()) {
            log.debug("BusinessException: {}", e.getMessage());
        } else {
            log.warn("BusinessException: {}", e.getMessage(), e);
        }
        return ResultUtils.error(e.getCode(),e.getMessage());
    }
    @ExceptionHandler(RuntimeException.class)
    public BaseResponse<?> handler(RuntimeException e) {
        log.error("RuntimeException", e);
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR,e.getMessage());
    }
}
