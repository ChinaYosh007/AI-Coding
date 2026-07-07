package com.yosh.coding.artificalIntelligence.model;

import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

@Data
@Description("this is result of that create HTML,Js and CSS!")
public class MultiFileCodeResult {
    @Description("this is result of that create HTML!")
    private String htmlCode;
    @Description("this is result of that create css!")
    private String cssCode;
    @Description("this is result of that create js!")
    private String jsCode;
    @Description("description your create code!")
    private String description;
}
