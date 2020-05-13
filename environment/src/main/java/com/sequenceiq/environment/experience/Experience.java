package com.sequenceiq.environment.experience;

import com.sequenceiq.environment.environment.dto.EnvironmentExperienceDto;

public interface Experience {

    boolean hasExistingClusterForEnvironment(EnvironmentExperienceDto environment);

    void deleteConnectedExperiences(EnvironmentExperienceDto dto);

    ExperienceSource getSource();

}
