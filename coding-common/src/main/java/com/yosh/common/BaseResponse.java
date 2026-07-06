package com.yosh.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BaseResponse<T> implements Serializable {
    private int code;
    private T data;
    private String message;
}
