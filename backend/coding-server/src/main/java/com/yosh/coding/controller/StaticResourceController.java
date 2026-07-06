package com.yosh.coding.controller;

import com.yosh.coding.service.AppVersionService;
import com.yosh.model.constants.AppConstant;
import com.yosh.model.entity.AppVersion;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;

@RestController
@RequestMapping("/static")
public class StaticResourceController {

    private static final String DEPLOY_ROOT_DIR = AppConstant.CODE_DEPLOY_ROOT_DIR;

    @Autowired
    private AppVersionService appVersionService;

    @GetMapping("/preview/{appId}/{version}/**")
    public ResponseEntity<Resource> servePreviewResource(
            @PathVariable Long appId,
            @PathVariable Long version,
            HttpServletRequest request) {
        try {
            AppVersion appVersion = appVersionService.getByAppIdAndVersion(appId, version);
            if (appVersion == null || appVersion.getSourcePath() == null) {
                return ResponseEntity.notFound().build();
            }
            File sourceDir = new File(appVersion.getSourcePath());
            if (!sourceDir.exists() || !sourceDir.isDirectory()) {
                return ResponseEntity.notFound().build();
            }
            String resourcePath = getResourcePath(request, "/static/preview/" + appId + "/" + version);
            if (resourcePath.isEmpty()) {
                return redirectToSlash(request);
            }
            return getResourceResponse(sourceDir, resourcePath);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{deployKey}/**")
    public ResponseEntity<Resource> serveStaticResource(
            @PathVariable String deployKey,
            HttpServletRequest request) {
        try {
            String resourcePath = getResourcePath(request, "/static/" + deployKey);
            if (resourcePath.isEmpty()) {
                return redirectToSlash(request);
            }
            File rootDir = new File(DEPLOY_ROOT_DIR, deployKey);
            return getResourceResponse(rootDir, resourcePath);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private String getResourcePath(HttpServletRequest request, String prefix) {
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();
        String pathWithinContext = requestUri.substring(contextPath.length());
        return pathWithinContext.substring(prefix.length());
    }

    private ResponseEntity<Resource> redirectToSlash(HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Location", request.getRequestURI() + "/");
        return new ResponseEntity<>(headers, HttpStatus.MOVED_PERMANENTLY);
    }

    private ResponseEntity<Resource> getResourceResponse(File rootDir, String resourcePath) throws Exception {
        if (resourcePath.equals("/")) {
            resourcePath = "/index.html";
        }
        File file = new File(rootDir, resourcePath);
        String rootPath = rootDir.getCanonicalPath();
        String filePath = file.getCanonicalPath();
        if (!filePath.equals(rootPath) && !filePath.startsWith(rootPath + File.separator)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (!file.exists() || !file.isFile()) {
            return ResponseEntity.notFound().build();
        }
        Resource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
                .header("Content-Type", getContentTypeWithCharset(filePath))
                .body(resource);
    }

    private String getContentTypeWithCharset(String filePath) {
        if (filePath.endsWith(".html")) {
            return "text/html; charset=UTF-8";
        }
        if (filePath.endsWith(".css")) {
            return "text/css; charset=UTF-8";
        }
        if (filePath.endsWith(".js")) {
            return "application/javascript; charset=UTF-8";
        }
        if (filePath.endsWith(".png")) {
            return "image/png";
        }
        if (filePath.endsWith(".jpg") || filePath.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        return "application/octet-stream";
    }
}
