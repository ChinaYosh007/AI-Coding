package com.yosh.coding.artificalIntelligence.skill;

import cn.hutool.core.io.FileUtil;
import com.yosh.model.constants.AppConstant;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;

class WriteToFileTest {

    @Test
    void writeToFileShouldUseBoundAppVersionDirectory() {
        long appId = 999999000001L;
        long version = 7L;
        File projectDir = new File(AppConstant.CODE_OUTPUT_ROOT_DIR,
                AppConstant.VUE_PREFIX + appId + "_" + version);

        FileUtil.del(projectDir);
        try {
            WriteToFile writeToFile = new WriteToFile(appId, version);
            String result = writeToFile.writeToFile("src/App.vue", "<template>ok</template>");

            File appVue = new File(projectDir, "src/App.vue");
            Assertions.assertTrue(result.startsWith("Wrote to file"));
            Assertions.assertTrue(appVue.exists());
            Assertions.assertEquals("<template>ok</template>", FileUtil.readUtf8String(appVue));
        } finally {
            FileUtil.del(projectDir);
        }
    }
}
