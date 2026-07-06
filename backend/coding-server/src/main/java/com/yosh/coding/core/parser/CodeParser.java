package com.yosh.coding.core.parser;

public interface CodeParser<T>{
    T parseCode(String body);
}
