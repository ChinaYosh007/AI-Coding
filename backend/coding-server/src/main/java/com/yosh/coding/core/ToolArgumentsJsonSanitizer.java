package com.yosh.coding.core;

/**
 * Repairs non-standard JSON emitted by some OpenAI-compatible tool-calling APIs.
 *
 * <p>Tool arguments are themselves a JSON document. Some providers decode line
 * breaks too early or emit the non-JSON escape sequence {@code \'}. This class
 * restores valid JSON before LangChain4j deserializes the arguments.</p>
 */
final class ToolArgumentsJsonSanitizer {

    private ToolArgumentsJsonSanitizer() {
    }

    static String sanitize(String arguments) {
        if (arguments == null || arguments.isEmpty()) {
            return arguments;
        }

        StringBuilder result = new StringBuilder(arguments.length() + 32);
        boolean inString = false;
        boolean escaping = false;
        for (int index = 0; index < arguments.length(); index++) {
            char current = arguments.charAt(index);

            if (!inString) {
                result.append(current);
                if (current == '"') {
                    inString = true;
                }
                continue;
            }

            if (escaping) {
                if (current == '\'') {
                    result.append(current);
                } else if (current == '"' || current == '\\' || current == '/' || current == 'b'
                        || current == 'f' || current == 'n' || current == 'r' || current == 't' || current == 'u') {
                    result.append('\\').append(current);
                } else {
                    result.append("\\\\").append(current);
                }
                escaping = false;
                continue;
            }

            if (current == '\\') {
                escaping = true;
            } else if (current == '"') {
                result.append(current);
                inString = false;
            } else if (current == '\r') {
                if (index + 1 < arguments.length() && arguments.charAt(index + 1) == '\n') {
                    index++;
                }
                result.append("\\n");
            } else if (current == '\n') {
                result.append("\\n");
            } else if (current == '\t') {
                result.append("\\t");
            } else {
                result.append(current);
            }
        }

        if (escaping) {
            result.append("\\\\");
        }
        return result.toString();
    }
}
