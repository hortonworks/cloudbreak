package com.sequenceiq.environment.experience.common;

import java.util.LinkedList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("experience.scan.config")
public class XPServices {

    private List<CommonExperience> experiences;

    public List<CommonExperience> getExperiences() {
        return experiences != null ? experiences : new LinkedList<>();
    }

    public void setExperiences(List<CommonExperience> experiences) {
        this.experiences = experiences;
    }

}
