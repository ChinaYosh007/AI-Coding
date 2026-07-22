package com.yosh.coding.core.parser;

import com.yosh.coding.artificalIntelligence.model.MultiFileCodeResult;
import com.yosh.exception.BusinessException;
import com.yosh.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class MultiFIleCodeParser implements  CodeParser<MultiFileCodeResult>{
    private static final Pattern HTML_CODE_PATTERN = Pattern.compile(
            "```[ \\t]*(?:html|index\\.html)[ \\t]*\\R([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);
    private static final Pattern CSS_CODE_PATTERN = Pattern.compile(
            "```[ \\t]*(?:css|style\\.css)[ \\t]*\\R([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);
    private static final Pattern JS_CODE_PATTERN = Pattern.compile(
            "```[ \\t]*(?:js|javascript|script\\.js)[ \\t]*\\R([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);

    @Override
    public MultiFileCodeResult parseCode(String content) {
        if (content == null || content.isBlank()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 返回的多文件代码为空，请重试");
        }
        MultiFileCodeResult result = new MultiFileCodeResult();
        log.debug("Parsing code content length: {}", content.length());

        // 提取各类代码
        String htmlCode = extractCodeByPattern(content, HTML_CODE_PATTERN);
        String cssCode = extractCodeByPattern(content, CSS_CODE_PATTERN);
        String jsCode = extractCodeByPattern(content, JS_CODE_PATTERN);

        log.debug("Extracted - HTML: {}, CSS: {}, JS: {}",
                htmlCode != null ? htmlCode.length() : 0,
                cssCode != null ? cssCode.length() : 0,
                jsCode != null ? jsCode.length() : 0);
        if (htmlCode == null || cssCode == null || jsCode == null) {
            log.warn("Incomplete multi-file response: html={}, css={}, js={}",
                    htmlCode != null, cssCode != null, jsCode != null);
        }

        // 设置HTML代码
        if (htmlCode != null && !htmlCode.trim().isEmpty()) {
            result.setHtmlCode(htmlCode.trim());
        }
        // 设置CSS代码
        if (cssCode != null && !cssCode.trim().isEmpty()) {
            result.setCssCode(cssCode.trim());
        }
        // 设置JS代码
        if (jsCode != null && !jsCode.trim().isEmpty()) {
            result.setJsCode(jsCode.trim());
        }
        return result;
    }

    /**
     * 根据正则模式提取代码
     *
     * @param content 原始内容
     * @param pattern 正则模式
     * @return 提取的代码
     */
    private static String extractCodeByPattern(String content, Pattern pattern) {
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}
