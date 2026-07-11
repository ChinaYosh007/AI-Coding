package com.yosh.coding.service;

import org.springframework.web.multipart.MultipartFile;

public interface OssUploadService {
    String uploadImage(MultipartFile file);
}
