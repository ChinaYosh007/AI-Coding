package com.yosh.coding.agent.model.image.query;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResourceCollectionResult {

    private List<ImageResource> resources;

    private List<String> usageSuggestions;

    private List<String> warnings;
}