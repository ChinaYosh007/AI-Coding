package com.yosh.coding.core.saver;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Last-line protection for generated source: placeholder image providers must
 * never be persisted even when a model ignores its image-resource prompt.
 */
public final class PlaceholderImageUrlSanitizer {

    private static final Pattern PLACEHOLDER_IMAGE_URL = Pattern.compile(
            "https?://(?:picsum\\.photos|loremflickr\\.com|placehold\\.(?:co|it)|images\\.unsplash\\.com)[^\\s\\\"'()]*",
            Pattern.CASE_INSENSITIVE);

    private static final String FALLBACK_IMAGE_URL =
            "data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI4MDAiIGhlaWdodD0iNTAwIiB2aWV3Qm94PSIwIDAgODAwIDUwMCI+PGRlZnM+PGxpbmVhckdyYWRpZW50IGlkPSJnIiB4MT0iMCIgeTE9IjAiIHgyPSIxIiB5Mj0iMSI+PHN0b3Agc3RvcC1jb2xvcj0iIzBmMTcyYSIvPjxzdG9wIG9mZnNldD0iMSIgc3RvcC1jb2xvcj0iIzMzNDE1NSIvPjwvbGluZWFyR3JhZGllbnQ+PC9kZWZzPjxyZWN0IHdpZHRoPSI4MDAiIGhlaWdodD0iNTAwIiBmaWxsPSJ1cmwoI2cpIi8+PC9zdmc+";

    private static final ThreadLocal<List<String>> REPLACEMENT_URLS =
            ThreadLocal.withInitial(List::of);

    private PlaceholderImageUrlSanitizer() {
    }

    public static String sanitize(String content) {
        return sanitize(content, REPLACEMENT_URLS.get());
    }

    public static String sanitize(String content, List<String> replacementUrls) {
        if (content == null || content.isBlank()) {
            return content;
        }
        Matcher matcher = PLACEHOLDER_IMAGE_URL.matcher(content);
        if (!matcher.find()) {
            return content;
        }

        List<String> usableUrls = replacementUrls == null ? List.of() : replacementUrls.stream()
                .filter(url -> url != null && url.startsWith("http"))
                .distinct()
                .toList();
        StringBuffer sanitized = new StringBuffer();
        int index = 0;
        do {
            String replacement = usableUrls.isEmpty()
                    ? FALLBACK_IMAGE_URL
                    : usableUrls.get(index++ % usableUrls.size());
            matcher.appendReplacement(sanitized, Matcher.quoteReplacement(replacement));
        } while (matcher.find());
        matcher.appendTail(sanitized);
        return sanitized.toString();
    }

    public static <T> T withReplacementUrls(List<String> replacementUrls, Supplier<T> action) {
        List<String> previous = REPLACEMENT_URLS.get();
        REPLACEMENT_URLS.set(replacementUrls == null ? List.of() : new ArrayList<>(replacementUrls));
        try {
            return action.get();
        } finally {
            REPLACEMENT_URLS.set(previous);
        }
    }
}
