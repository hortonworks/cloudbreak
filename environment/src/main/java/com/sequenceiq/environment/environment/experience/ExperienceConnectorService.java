package com.sequenceiq.environment.environment.experience;

import com.sequenceiq.environment.environment.domain.Environment;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExperienceConnectorService {

    private final List<Experience> experiences;

    public ExperienceConnectorService(List<Experience> experiences) {
        this.experiences = experiences;
    }

    public long getConnectedExperienceQuantity(Environment environment) {
        return experiences
                .stream()
                .filter(experience -> experience.hasExistingClusterForEnvironment(environment))
                .count();
    }

}
