package com.yosh.common;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OssEntry {
    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String bucketName;

}
