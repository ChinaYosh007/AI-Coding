package com.yosh.coding.controller;

import com.yosh.coding.service.OssUploadService;
import com.yosh.coding.service.UserService;
import com.yosh.common.BaseResponse;
import com.yosh.common.ResultUtils;
import com.yosh.exception.ErrorCode;
import com.yosh.exception.ThrowUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

@RestController
@RequestMapping("/file")
public class UploadController {

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            MediaType.IMAGE_JPEG_VALUE,
            MediaType.IMAGE_PNG_VALUE,
            "image/webp"
    );

    @Resource
    private UserService userService;

    @Resource
    private OssUploadService ossUploadService;

    /**
     * 上传图片到阿里云 OSS
     */
    @PostMapping(value = "/upload-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BaseResponse<String> uploadImage(@RequestPart("file") MultipartFile file,
                                            HttpServletRequest request) {
        userService.getLoginUser(request);
        ThrowUtils.throwIf(file == null || file.isEmpty(),
                ErrorCode.PARAMS_ERROR, "请选择需要上传的图片");
        String contentType = file.getContentType();
        ThrowUtils.throwIf(contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType),
                ErrorCode.PARAMS_ERROR, "仅支持 JPG、PNG、WebP 图片");

        String imageUrl = ossUploadService.uploadImage(file);
        return ResultUtils.success(imageUrl);
    }

    /**
     * 上传文件到阿里云 OSS（支持 PDF、文档等任意类型）
     */
    @PostMapping(value = "/upload-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BaseResponse<String> uploadFile(@RequestPart("file") MultipartFile file,
                                          HttpServletRequest request) {
        userService.getLoginUser(request);
        ThrowUtils.throwIf(file == null || file.isEmpty(),
                ErrorCode.PARAMS_ERROR, "请选择需要上传的文件");
        long maxFileSize = 10 * 1024 * 1024L;
        ThrowUtils.throwIf(file.getSize() > maxFileSize,
                ErrorCode.PARAMS_ERROR, "文件大小不能超过 10MB");

        String fileUrl = ossUploadService.uploadFile(file);
        return ResultUtils.success(fileUrl);
    }
}
