package com.yosh.coding.core;

import com.yosh.coding.agent.model.image.query.ImageResource;
import com.yosh.coding.agent.model.image.query.ResourceCollectionResult;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ResourcePromptAssembler {

    public String assemble(String userPrompt, ResourceCollectionResult result) {
        StringBuilder prompt = new StringBuilder(userPrompt).append("""

                <image_asset_policy>
                This is a mandatory rule. Never use or invent an external placeholder image URL, including picsum.photos, loremflickr.com, placehold.co, placehold.it, or guessed Unsplash URLs.
                An img src or CSS background-image URL must exactly match a URL in available_resources below. If no suitable collected resource exists, use a CSS gradient, a solid color, an inline SVG, or text initials instead of an external image.
                </image_asset_policy>

                <available_resources>
                The following up to 50 real resources were collected by the system. Before writing source code, assign these URLs to the hero, carousel, cards, articles, team avatars, client area, and logo as needed.
                Every visual URL in generated source must be copied exactly from this list. Reuse a collected URL when there are more visual slots than resources; do not create a new URL.
                Use CONTENT and ILLUSTRATION resources for page images, and the LOGO resource for the brand mark.
                """);

        if (result != null && result.getResources() != null) {
            result.getResources().stream()
                    .filter(resource -> resource != null && resource.getImageUrl() != null && !resource.getImageUrl().isBlank())
                    .limit(50)
                    .forEach(resource -> appendResource(prompt, resource));
        }

        List<String> suggestions = result == null ? null : result.getUsageSuggestions();
        if (suggestions != null && !suggestions.isEmpty()) {
            prompt.append("Usage suggestions:\n");
            suggestions.stream()
                    .filter(suggestion -> suggestion != null && !suggestion.isBlank())
                    .limit(50)
                    .forEach(suggestion -> prompt.append("- ").append(suggestion).append('\n'));
        }

        List<String> warnings = result == null ? null : result.getWarnings();
        if (warnings != null && !warnings.isEmpty()) {
            prompt.append("Unavailable resource sources (use collected URLs or CSS fallback instead):\n");
            warnings.stream()
                    .filter(warning -> warning != null && !warning.isBlank())
                    .limit(10)
                    .forEach(warning -> prompt.append("- ").append(warning).append('\n'));
        }

        return prompt.append("</available_resources>").toString();
    }

    private void appendResource(StringBuilder prompt, ImageResource resource) {
        String category = resource.getImageCategory() == null ? "IMAGE" : resource.getImageCategory().name();
        String description = resource.getDescription() == null ? "Visual resource" : resource.getDescription();
        prompt.append("- [").append(category).append("] ")
                .append(description)
                .append(" | URL: ").append(resource.getImageUrl())
                .append('\n');
    }
}
