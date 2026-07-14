package com.yosh.coding.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;

public interface OssUploadService {
    String uploadImage(MultipartFile file);

    String uploadFile(MultipartFile file, String fileName);
    String uploadFile(MultipartFile file);
    String uploadFile(File file, String fileName);
}
