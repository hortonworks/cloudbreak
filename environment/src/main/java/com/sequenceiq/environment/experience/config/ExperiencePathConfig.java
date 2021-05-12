package com.sequenceiq.environment.experience.config;

import static java.util.Collections.emptyMap;

import java.util.Map;

import javax.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("environment.experience.path")
public class ExperiencePathConfig {

    private Map<String, String> toReplace;

    public ExperiencePathConfig() {
        this(emptyMap());
    }

    public ExperiencePathConfig(Map<String, String> toReplace) {
        this.toReplace = toReplace;
    }

    @NotNull
    public Map<String, String> getToReplace() {
        return toReplace == null ? emptyMap() : toReplace;
    }

    public void setToReplace(Map<String, String> toReplace) {
        this.toReplace = toReplace;
    }

    @Override
    public String toString() {
        return "ExperiencePathConfig{" +
                "componentToReplace=" + getToReplace().toString() +
                '}';
    }

}
