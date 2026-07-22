package com.yosh.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {
    SUCCESS(0, "ok"),
    TOO_MANY_REQUEST(42900,"请求过于频繁，请稍后再试"),
    PARAMS_ERROR(40000, "请求参数错误"),
    NOT_LOGIN_ERROR(40100, "未登录"),
    NO_AUTH_ERROR(40101, "无权限"),
    NOT_FOUND_ERROR(40400, "请求数据不存在"),
    FORBIDDEN_ERROR(40300, "禁止访问"),
    SYSTEM_ERROR(50000, "系统内部异常"),
    OPERATION_ERROR(50001, "操作失败"),
    PASSWORD_DIFFERENCE(50002,"密码不一致"),
    USER_ALREARLY_HAVE(50003,"用户已存在" ),
    ADD_USER_ERROR(50004, "添加用户失败"), ERROR_QUERY(50005, "查询失败");

    /**
     * 状态码
     */
    private final int code;

    /**
     * 信息
     */
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
