package com.sequenceiq.environment.experience.config;

import java.util.LinkedList;
import java.util.List;

import javax.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.sequenceiq.environment.experience.common.CommonExperience;

@Component
@ConfigurationProperties("environment.experience")
public class ExperienceServicesConfig {

    private List<CommonExperience> configs;

    @NotNull
    public List<CommonExperience> getConfigs() {
        return configs != null ? configs : new LinkedList<>();
    }

    public void setConfigs(List<CommonExperience> configs) {
        this.configs = configs;
    }

}
