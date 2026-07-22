package com.yosh.coding.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.ObjectMetadata;
import com.yosh.coding.service.OssUploadService;
import com.yosh.common.OssEntry;
import com.yosh.exception.BusinessException;
import com.yosh.exception.ErrorCode;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;

@Service
@Slf4j
public class UploadServiceImpl implements OssUploadService {

    @Resource
    private OSS ossClient;

    @Resource
    private OssEntry ossEntry;

    @Override
    public String uploadImage(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "无法识别图片类型");
        }

        String objectName = buildObjectName(contentType);
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(contentType);
        metadata.setContentLength(file.getSize());

        try (InputStream inputStream = file.getInputStream()) {
            ossClient.putObject(ossEntry.getBucketName(), objectName, inputStream, metadata);
            return buildUrl(objectName);
        } catch (IOException e) {
            log.error("读取上传图片失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "读取上传图片失败");
        } catch (Exception e) {
            log.error("上传图片到 OSS 失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "图片上传失败");
        }
    }

    @Override
    public String uploadFile(MultipartFile file) {
        return uploadFile(file, null);
    }

    @Override
    public String uploadFile(File file, String fileName) {
        String originalFilename = file.getName();
        String extension = extractExtension(originalFilename);
        String objectName;
        if (fileName != null) {
            objectName = buildFileObjectName(fileName);
        } else {
            objectName = buildFileObjectName(extension);
        }
        ObjectMetadata metadata = new ObjectMetadata();
        String contentType = "application/octet-stream";
        if (contentType != null) {
            metadata.setContentType(contentType);
        }
        metadata.setContentLength((int) file.length());
        metadata.setContentDisposition("attachment; filename=\"" + originalFilename + "\"");

        try (InputStream inputStream = new FileInputStream(file)) {
            ossClient.putObject(ossEntry.getBucketName(), objectName, inputStream, metadata);
            return buildUrl(objectName);
        } catch (IOException e) {
            log.error("读取上传文件失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "读取上传文件失败");
        } catch (Exception e) {
            log.error("上传文件到 OSS 失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "文件上传失败");
        }
    }

    @Override
    public String uploadFile(MultipartFile file, String fileName) {
        String originalFilename = file.getOriginalFilename();
        String extension = extractExtension(originalFilename);
        String objectName;
        if (fileName != null) {
            objectName = buildFileObjectName(fileName);
        } else {
            objectName = buildFileObjectName(extension);
        }
        ObjectMetadata metadata = new ObjectMetadata();
        String contentType = file.getContentType();
        if (contentType != null) {
            metadata.setContentType(contentType);
        }
        metadata.setContentLength(file.getSize());
        metadata.setContentDisposition("attachment; filename=\"" + originalFilename + "\"");

        try (InputStream inputStream = file.getInputStream()) {
            ossClient.putObject(ossEntry.getBucketName(), objectName, inputStream, metadata);
            return buildUrl(objectName);
        } catch (IOException e) {
            log.error("读取上传文件失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "读取上传文件失败");
        } catch (Exception e) {
            log.error("上传文件到 OSS 失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "文件上传失败");
        }
    }

    @Override
    public boolean deleteFileByUrl(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return false;
        }

        URI fileUri;
        URI endpointUri;
        try {
            fileUri = URI.create(fileUrl);
            String endpoint = ossEntry.getEndpoint();
            endpointUri = URI.create(endpoint.contains("://") ? endpoint : "https://" + endpoint);
        } catch (IllegalArgumentException exception) {
            log.warn("忽略无法解析的 OSS 文件地址：{}", fileUrl);
            return false;
        }

        String expectedHost = ossEntry.getBucketName() + "." + endpointUri.getHost();
        if (fileUri.getHost() == null || !expectedHost.equalsIgnoreCase(fileUri.getHost())) {
            log.debug("文件不属于当前项目 OSS，跳过删除：{}", fileUrl);
            return false;
        }

        String objectName = fileUri.getPath();
        if (objectName == null || objectName.length() <= 1) {
            return false;
        }
        objectName = objectName.substring(1);

        try {
            ossClient.deleteObject(ossEntry.getBucketName(), objectName);
            return true;
        } catch (Exception exception) {
            log.error("删除 OSS 文件失败，url={}", fileUrl, exception);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "删除 OSS 文件失败");
        }
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "bin";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    private String buildFileObjectName(String extension) {
        LocalDate today = LocalDate.now();
        return "user-files/%d/%02d/%02d/%s.%s".formatted(
                today.getYear(),
                today.getMonthValue(),
                today.getDayOfMonth(),
                UUID.randomUUID(),
                extension
        );
    }

    private String buildObjectName(String contentType) {
        String extension = switch (contentType) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的图片类型");
        };
        LocalDate today = LocalDate.now();
        return "user-images/%d/%02d/%02d/%s.%s".formatted(
                today.getYear(),
                today.getMonthValue(),
                today.getDayOfMonth(),
                UUID.randomUUID(),
                extension
        );
    }

    private String buildUrl(String objectName) {
        String endpoint = ossEntry.getEndpoint().replaceFirst("^https?://", "");
        return "https://" + ossEntry.getBucketName() + "." + endpoint + "/" + objectName;
    }
}
