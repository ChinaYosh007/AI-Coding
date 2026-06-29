package com.yosh.coding.core.saver;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.yosh.exception.BusinessException;
import com.yosh.exception.ErrorCode;
import com.yosh.exception.ThrowUtils;
import com.yosh.model.enums.CodeGenTypeEnum;

import java.io.File;
import java.nio.charset.StandardCharsets;

public abstract class CodeSaverTemplate<T>{
    protected static final String URL = System.getProperty("user.dir") + "/src/main/resources/static/tmp/output_file";

    protected abstract CodeGenTypeEnum getCodeType();

    public  final File saveCode(T res ){
        valiedInput(res);
        String dir = builderPath();
        saveFile(res,dir);
        return  new File(dir);
    }

    protected abstract void saveFile(T res, String dir);

    protected  void valiedInput(T res){
        ThrowUtils.throwIf(res == null,new BusinessException(ErrorCode.SYSTEM_ERROR,"输入文件不能为空"));
    }

    protected  String builderPath(){
        String prefix =  getCodeType().getValue();
        String fileName = StrUtil.format("{}_{}",prefix, IdUtil.getSnowflakeNextIdStr());
        String dir = URL + File.separator + fileName;
        FileUtil.mkdir(dir);
        return dir;
    }

    protected static  void writeFile(String dir,String fileName,String resource) {
        String file = dir + File.separator + fileName;
        FileUtil.writeString(resource,file, StandardCharsets.UTF_8);
    }
}
