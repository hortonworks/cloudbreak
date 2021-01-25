package com.sequenceiq.environment.experience;

import javax.validation.constraints.NotNull;

import com.sequenceiq.environment.environment.dto.EnvironmentExperienceDto;

public interface Experience {

    /**
     * Operation that counts and returns the number of clusters/workspaces that
     * is connected to the environment given environment.
     *
     * @param environment the {@link EnvironmentExperienceDto} DTO class that contains the
     *                    necessary data for the deletion.
     *                    This object cannot be null since its content is can be crucial.
     * @return
     */
    int getConnectedClusterCountForEnvironment(EnvironmentExperienceDto environment);

    /**
     * Operation that invokes deletion on the implemented/designated experience.
     *
     * @param environment the {@link EnvironmentExperienceDto} DTO class that contains the
     *            necessary data for the deletion.
     *            This object cannot be null since its content is can be crucial.
     */
    void deleteConnectedExperiences(@NotNull EnvironmentExperienceDto environment);

    ExperienceSource getSource();

}
