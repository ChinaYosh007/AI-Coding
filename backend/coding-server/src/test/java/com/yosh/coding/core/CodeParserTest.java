package com.yosh.coding.core;

import com.yosh.coding.artificalIntelligence.model.HtmlCodeResult;
import com.yosh.coding.artificalIntelligence.model.MultiFileCodeResult;
import com.yosh.coding.core.parser.CodeParserExcutor;
import com.yosh.coding.core.saver.MultiFileCodeTemplate;
import com.yosh.exception.BusinessException;
import com.yosh.model.enums.CodeGenTypeEnum;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CodeParserTest {

    @Test
    void parseHtmlCode() {
        String codeContent = """
                随便写一段描述：
                html 格式
                <!DOCTYPE html>
                <html>
                <head>
                    <title>测试页面</title>
                </head>
                <body>
                    <h1>Hello World!</h1>
                </body>
                </html>

                随便写一段描述
                """;
        HtmlCodeResult result = (HtmlCodeResult) CodeParserExcutor.executeCode(codeContent, CodeGenTypeEnum.HTML);
        assertNotNull(result);
        assertNotNull(result.getHtmlCode());
    }

    @Test
    void parseMultiFileCode() {
        String codeContent = """
                创建一个完整的网页：

                ```html
                <!DOCTYPE html>
                <html>
                <head>
                    <title>多文件示例</title>
                    <link rel="stylesheet" href="style.css">
                </head>
                <body>
                    <h1>欢迎使用</h1>
                    <script src="script.js"></script>
                </body>
                </html>
                ```

                ```css
                h1 {
                    color: blue;
                    text-align: center;
                }
                ```

                ```js
                console.log('页面加载完成');
                ```

                文件创建完成！
                """;
        MultiFileCodeResult result = (MultiFileCodeResult) CodeParserExcutor.executeCode(codeContent, CodeGenTypeEnum.MULTI_FILE);
        assertNotNull(result);
        assertNotNull(result.getHtmlCode());
        assertNotNull(result.getCssCode());
        assertNotNull(result.getJsCode());
    }

    @Test
    void parseMultiFileCodeWithFileNameLabelsAndCrLf() {
        String codeContent = "```index.html\r\n<html><body>页面</body></html>\r\n```\r\n"
                + "```style.css\r\nbody { color: #222; }\r\n```\r\n"
                + "```script.js\r\nconsole.log('ready');\r\n```";

        MultiFileCodeResult result = (MultiFileCodeResult) CodeParserExcutor.executeCode(
                codeContent, CodeGenTypeEnum.MULTI_FILE);

        assertEquals("<html><body>页面</body></html>", result.getHtmlCode());
        assertEquals("body { color: #222; }", result.getCssCode());
        assertEquals("console.log('ready');", result.getJsCode());
    }

    @Test
    void rejectTruncatedMultiFileResponseBeforeWritingAnyFile() {
        String truncatedContent = """
                ```html
                <html><link rel="stylesheet" href="style.css"></html>
                ```
                ```css
                body {
                    color: #222;
                """;
        MultiFileCodeResult result = (MultiFileCodeResult) CodeParserExcutor.executeCode(
                truncatedContent, CodeGenTypeEnum.MULTI_FILE);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> new MultiFileCodeTemplate().saveCode(result, -1L, -1L));

        assertTrue(exception.getMessage().contains("style.css"));
        assertTrue(exception.getMessage().contains("script.js"));
    }
}
