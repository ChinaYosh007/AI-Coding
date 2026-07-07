package com.yosh.coding.service.impl;

import cn.hutool.core.io.FileUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.util.Set;
import java.util.zip.ZipFile;
import java.util.stream.Collectors;

class AppServiceImplTest {

    @Test
    void zipAppSourceShouldExcludeNodeModules() throws Exception {
        File sourceDir = FileUtil.createTempFile("vue-download-", "", true);
        FileUtil.del(sourceDir);
        FileUtil.mkdir(sourceDir);
        try {
            FileUtil.writeUtf8String("{}", new File(sourceDir, "package.json"));
            FileUtil.writeUtf8String("<template>ok</template>", new File(sourceDir, "src/App.vue"));
            FileUtil.writeUtf8String("ignored", new File(sourceDir, "node_modules/vue/index.js"));

            AppServiceImpl appService = new AppServiceImpl();
            File zipFile = ReflectionTestUtils.invokeMethod(appService, "zipAppSource", sourceDir, 1L, 2L);

            Assertions.assertNotNull(zipFile);
            Assertions.assertTrue(zipFile.exists());
            try (ZipFile zip = new ZipFile(zipFile)) {
                Set<String> entryNames = zip.stream().map(entry -> entry.getName()).collect(Collectors.toSet());
                Assertions.assertTrue(entryNames.contains("package.json"));
                Assertions.assertTrue(entryNames.contains("src/App.vue"));
                Assertions.assertFalse(entryNames.contains("node_modules/vue/index.js"));
            } finally {
                FileUtil.del(zipFile);
            }
        } finally {
            FileUtil.del(sourceDir);
        }
    }
}
