package com.yosh.coding.artificalIntelligence.guardrail;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.InputGuardrailResult;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class PromptSafetyInputGuardrail implements InputGuardrail {

    // 敏感词列表
    private static final List<String> SENSITIVE_WORDS = Arrays.asList(
            "忽略之前的指令", "忽略提示", "忽略提示词", "忽略系统提示",
            "无视提示", "无视提示词", "无视系统提示",
            "ignore previous instructions", "ignore above",
            "破解", "hack", "绕过", "bypass", "越狱", "jailbreak"
    );

    // 注入攻击模式
    private static final List<Pattern> INJECTION_PATTERNS = Arrays.asList(
            Pattern.compile("(?i)ignore\\s+(?:previous|above|all)\\s+(?:instructions?|commands?|prompts?)"),
            Pattern.compile("(?i)(?:forget|disregard)\\s+(?:everything|all)\\s+(?:above|before)"),
            Pattern.compile("(?i)(?:pretend|act|behave)\\s+(?:as|like)\\s+(?:if|you\\s+are)"),
            Pattern.compile("(?i)system\\s*:\\s*you\\s+are"),
            Pattern.compile("(?i)new\\s+(?:instructions?|commands?|prompts?)\\s*:"),
            Pattern.compile("(?:忽略|无视|跳过|绕过|覆盖|取消)\\s*(?:(?:之前|上面|上述|系统|原有|所有)\\s*)?(?:的\\s*)?(?:提示(?:词)?|指令|规则|命令)")
    );

    @Override
    public InputGuardrailResult validate(UserMessage userMessage) {
        String input = userMessage.singleText();
        // 检查输入长度
        if (input.length() > 1000) {
            return fatal("输入内容过长，不要超过 1000 字");
        }
        // 检查是否为空
        if (input.trim().isEmpty()) {
            return fatal("输入内容不能为空");
        }
        // 检查敏感词
        String lowerInput = input.toLowerCase(Locale.ROOT);
        for (String sensitiveWord : SENSITIVE_WORDS) {
            if (lowerInput.contains(sensitiveWord.toLowerCase(Locale.ROOT))) {
                return fatal("输入包含不当内容，请修改后重试");
            }
        }
        // 检查注入攻击模式
        for (Pattern pattern : INJECTION_PATTERNS) {
            if (pattern.matcher(input).find()) {
                return fatal("检测到恶意输入，请求被拒绝");
            }
        }
        return success();
    }
}
