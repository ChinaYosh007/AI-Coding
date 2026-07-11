import com.yosh.common.OssEntry;
import com.yosh.utils.ScreenshotUtil;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
@Deprecated
public class ScreenshotTest {
    @Resource
    private OssEntry ossEntry;
    @Test
    public void testCreateCover() throws IOException {
        byte[] screenshotBytes = ScreenshotUtil.getScreenshotBytes("https://www.baidu.com");
        String ossUrl = ScreenshotUtil.saveScreenshot(screenshotBytes, ossEntry);
        assertNotNull(ossUrl);
    }
}
