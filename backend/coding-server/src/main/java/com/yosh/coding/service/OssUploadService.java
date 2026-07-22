package com.yosh.coding.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;

public interface OssUploadService {
    String uploadImage(MultipartFile file);

    String uploadFile(MultipartFile file, String fileName);
    String uploadFile(MultipartFile file);
    String uploadFile(File file, String fileName);

    /**
     * 删除当前项目 OSS Bucket 中的文件。
     *
     * @param fileUrl 文件完整 URL
     * @return URL 属于当前项目 OSS 并已提交删除时返回 true，否则返回 false
     */
    boolean deleteFileByUrl(String fileUrl);
}
