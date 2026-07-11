package com.yosh.coding.artificalIntelligence.skill;

import com.fasterxml.jackson.databind.ser.Serializers;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Deprecated
@Description("由于技能管理类中采用了软件版本管理，所以无法制造bean,需要进行动态传递数值")
public class SkillManage {
    private final Map<String, BaseTool> skillMap = new HashMap<>();
    @Resource
    private BaseTool[] baseTool;
    @Bean
    public void init() {
        for (BaseTool tool : baseTool) {
            skillMap.put(tool.getToolName(), tool);
        }
        log.info("skillMap: {}", skillMap);
    }

    public BaseTool getSkill(String skillName) {
        return skillMap.get(skillName);
    }

}
