package com.sequenceiq.environment.experience;

import java.util.Set;

import javax.validation.constraints.NotNull;

import com.sequenceiq.environment.environment.dto.EnvironmentExperienceDto;

public interface Experience {

    /**
     * Operation that returns the clusters/workspaces that
     * is connected to the given environment.
     *
     * @param environment the {@link EnvironmentExperienceDto} DTO class that contains the
     *                    necessary data for the deletion.
     *                    This object cannot be null since its content is can be crucial.
     * @return connected clusters
     */
    Set<ExperienceCluster> getConnectedClustersForEnvironment(EnvironmentExperienceDto environment);

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
