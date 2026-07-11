package com.yosh.utils;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ObjectMetadata;
import com.yosh.common.OssEntry;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.filters.Canvas;
import net.coobird.thumbnailator.geometry.Positions;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
public class ScreenshotUtil {
    private static final int VIEWPORT_WIDTH = 1440;
    private static final int VIEWPORT_HEIGHT = 900;
    private static final int COVER_WIDTH = 800;
    private static final int COVER_HEIGHT = 600;
    private static final double COVER_QUALITY = 0.75;
    private static final ThreadLocal<ChromeDriver> threadLocal = new ThreadLocal<>();

    private static ChromeDriver initChrome() {
        ChromeOptions chrome = new ChromeOptions();
        chrome.addArguments("--headless=new", "--disable-gpu", "--no-sandbox");
        chrome.addArguments("--window-size=" + VIEWPORT_WIDTH + "," + VIEWPORT_HEIGHT);
        chrome.addArguments("--disable-infobars", "--disable-extensions");
        chrome.setExperimentalOption("excludeSwitches", List.of("enable-automation"));

        ChromeDriverService service = new ChromeDriverService.Builder()
                .withSilent(true)
                .build();
        return new ChromeDriver(service, chrome);
    }

    public static ChromeDriver getDriver() {
        ChromeDriver driver = threadLocal.get();
        if (driver == null) {
            driver = initChrome();
            threadLocal.set(driver);
        }
        return driver;
    }
    public static void destroy() {
        ChromeDriver driver = threadLocal.get();
        if (driver != null) {
            driver.quit();
            threadLocal.remove();
        }
    }

    public static byte[] getScreenshotBytes(String url) {
        ChromeDriver chrome = getDriver();
        try {
            chrome.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(10));
            chrome.get(url);
            waitForPageResources(chrome);

            Map<String, Object> layoutMetrics = chrome.executeCdpCommand("Page.getLayoutMetrics", Map.of());
            @SuppressWarnings("unchecked")
            Map<String, Number> contentSize = (Map<String, Number>) layoutMetrics.get("cssContentSize");
            if (contentSize == null) {
                contentSize = (Map<String, Number>) layoutMetrics.get("contentSize");
            }
            if (contentSize == null) {
                throw new IllegalStateException("Chrome did not return page layout metrics");
            }
            Map<String, Object> clip = Map.of(
                    "x", 0,
                    "y", 0,
                    "width", contentSize.get("width").doubleValue(),
                    "height", contentSize.get("height").doubleValue(),
                    "scale", 1
            );
            Map<String, Object> screenshot = chrome.executeCdpCommand("Page.captureScreenshot", Map.of(
                    "format", "png",
                    "fromSurface", true,
                    "captureBeyondViewport", true,
                    "clip", clip
            ));
            Object data = screenshot.get("data");
            if (!(data instanceof String encodedScreenshot)) {
                throw new IllegalStateException("Chrome did not return screenshot data");
            }
            return Base64.getDecoder().decode(encodedScreenshot);
        } finally {
            destroy();
        }
    }

    private static void waitForPageResources(ChromeDriver chrome) {
        chrome.manage().timeouts().scriptTimeout(Duration.ofSeconds(10));
        ((JavascriptExecutor) chrome).executeAsyncScript("""
                const done = arguments[arguments.length - 1];
                const images = Array.from(document.images)
                    .filter(image => !image.complete)
                    .map(image => new Promise(resolve => {
                        image.addEventListener('load', resolve, { once: true });
                        image.addEventListener('error', resolve, { once: true });
                    }));
                const fonts = document.fonts ? document.fonts.ready : Promise.resolve();
                Promise.race([
                    Promise.all([fonts, ...images]),
                    new Promise(resolve => setTimeout(resolve, 5000))
                ]).then(() => {
                    window.scrollTo(0, 0);
                    requestAnimationFrame(() => requestAnimationFrame(done));
                });
                """);
    }

    public static String saveScreenshot(byte[] screenshot, OssEntry ossEntry) throws IOException {
        BufferedImage source = ImageIO.read(new ByteArrayInputStream(screenshot));
        if (source == null) {
            throw new IOException("Screenshot data is not a supported image");
        }

        byte[] compressed = createCoverBytes(source);

        String fileName = System.currentTimeMillis() + "-" + UUID.randomUUID() + ".jpg";
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType("image/jpeg");
        metadata.setContentLength(compressed.length);
        OSS ossClient = new OSSClientBuilder()
                .build(ossEntry.getEndpoint(), ossEntry.getAccessKeyId(), ossEntry.getAccessKeySecret());
        try {
            ossClient.putObject(
                    ossEntry.getBucketName(),
                    fileName,
                    new ByteArrayInputStream(compressed),
                    metadata
            );
            return fileName;
        } catch (Exception e) {
            log.error("Failed to save screenshot to OSS", e);
            throw new IOException("Failed to save screenshot to OSS", e);
        } finally {
            ossClient.shutdown();
        }
    }

    public static byte[] createCoverBytes(BufferedImage source) throws IOException {
        BufferedImage cover = Thumbnails.of(source)
                .size(COVER_WIDTH, COVER_HEIGHT)
                .keepAspectRatio(true)
                .addFilter(new Canvas(COVER_WIDTH, COVER_HEIGHT, Positions.CENTER, Color.WHITE))
                .asBufferedImage();

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Thumbnails.of(cover)
                    .scale(1)
                    .outputFormat("jpg")
                    .outputQuality(COVER_QUALITY)
                    .toOutputStream(output);
            return output.toByteArray();
        }
    }

}
