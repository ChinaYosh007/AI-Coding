package com.yosh.coding.controller;

import cn.hutool.core.io.FileUtil;
import com.yosh.coding.service.AppVersionService;
import com.yosh.common.BaseResponse;
import com.yosh.model.entity.AppVersion;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.util.List;

class StaticResourceControllerTest {

    @Test
    void listPreviewSourceFilesShouldMatchFrontendFilesPath() {
        File sourceDir = FileUtil.createTempFile("vue-source-", "", true);
        FileUtil.del(sourceDir);
        FileUtil.mkdir(sourceDir);
        try {
            FileUtil.writeUtf8String("{}", new File(sourceDir, "package.json"));
            FileUtil.writeUtf8String("<template>ok</template>", new File(sourceDir, "src/App.vue"));
            FileUtil.writeUtf8String("ignored", new File(sourceDir, "node_modules/vue/index.js"));

            AppVersion appVersion = new AppVersion();
            appVersion.setAppId(1L);
            appVersion.setVersion(2L);
            appVersion.setSourcePath(sourceDir.getAbsolutePath());

            AppVersionService appVersionService = Mockito.mock(AppVersionService.class);
            Mockito.when(appVersionService.getByAppIdAndVersion(1L, 2L)).thenReturn(appVersion);

            StaticResourceController controller = new StaticResourceController();
            ReflectionTestUtils.setField(controller, "appVersionService", appVersionService);

            BaseResponse<List<String>> response = controller.listPreviewSourceFiles(1L, 2L);

            Assertions.assertEquals(0, response.getCode());
            Assertions.assertTrue(response.getData().contains("package.json"));
            Assertions.assertTrue(response.getData().contains("src/App.vue"));
            Assertions.assertFalse(response.getData().contains("node_modules/vue/index.js"));
        } finally {
            FileUtil.del(sourceDir);
        }
    }
}
