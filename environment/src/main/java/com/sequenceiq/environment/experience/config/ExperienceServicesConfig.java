package com.sequenceiq.environment.experience.config;

import java.util.LinkedList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.sequenceiq.environment.experience.common.CommonExperience;

@Component
@ConfigurationProperties("experience.scan.config")
public class ExperienceServicesConfig {

    private List<CommonExperience> experiences;

    public List<CommonExperience> getExperiences() {
        return experiences != null ? experiences : new LinkedList<>();
    }

    public void setExperiences(List<CommonExperience> experiences) {
        this.experiences = experiences;
    }

}
