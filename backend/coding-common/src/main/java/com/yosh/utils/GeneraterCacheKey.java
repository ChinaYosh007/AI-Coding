package com.yosh.utils;

import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONUtil;

public class GeneraterCacheKey {

    /**
     *  将传递的对象进行md5加密并且返回相关的key
     * @param value
     * @return
     */
    public static String generateCacheKey(Object value) {
       if(value == null) return DigestUtil.md5Hex("null");
       String json = JSONUtil.toJsonStr(value);
       return DigestUtil.md5Hex(json);
    }
}
