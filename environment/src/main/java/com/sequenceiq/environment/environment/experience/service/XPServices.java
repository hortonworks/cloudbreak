package com.sequenceiq.environment.environment.experience.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.sequenceiq.environment.environment.experience.resolve.Experience;

@Component
@ConfigurationProperties("xpservices")
public class XPServices {

    private Map<String, Experience> experiences = new HashMap<>();

    /**
     * @return the value of the internal map if it's not null. Otherwise an empty map will return.
     */
    public Map<String, Experience> getExperiences() {
        return experiences != null ? experiences : new HashMap<>();
    }

    public void setExperiences(Map<String, Experience> experiences) {
        this.experiences = experiences;
    }

}
