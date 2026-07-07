package com.yosh.utils;

import org.springframework.util.DigestUtils;

public class UserUtils {

    public static String getEncryptPassword(String userPassword) {
        // 盐值，混淆密码
        final String SALT = "I love coding with myself";
        return DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
    }

}
