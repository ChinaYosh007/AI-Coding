package com.yosh.coding.service.impl;

import com.aliyun.oss.OSS;
import com.yosh.common.OssEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class UploadServiceImplTest {

    @Mock
    private OSS ossClient;

    private UploadServiceImpl uploadService;

    @BeforeEach
    void setUp() {
        uploadService = new UploadServiceImpl();
        ReflectionTestUtils.setField(uploadService, "ossClient", ossClient);
        ReflectionTestUtils.setField(uploadService, "ossEntry", OssEntry.builder()
                .endpoint("https://oss-cn-hangzhou.aliyuncs.com")
                .bucketName("test-bucket")
                .build());
    }

    @Test
    void shouldDeleteFileOwnedByCurrentBucket() {
        boolean deleted = uploadService.deleteFileByUrl(
                "https://test-bucket.oss-cn-hangzhou.aliyuncs.com/user-images/2026/test.jpg"
        );

        assertTrue(deleted);
        verify(ossClient).deleteObject("test-bucket", "user-images/2026/test.jpg");
    }

    @Test
    void shouldIgnoreExternalFileUrl() {
        boolean deleted = uploadService.deleteFileByUrl("https://example.com/test.jpg");

        assertFalse(deleted);
        verifyNoInteractions(ossClient);
    }
}
