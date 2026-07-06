package com.yosh.coding.artificalIntelligence.model;

import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

@Data
@Description("this is result of that create HTML!")
public class HtmlCodeResult {

    @Description("HTML code")
    private String htmlCode;

    @Description("description your create HTML code")
    private String description;
}
