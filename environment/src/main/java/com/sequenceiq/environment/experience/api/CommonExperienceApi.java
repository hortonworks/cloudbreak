package com.sequenceiq.environment.experience.api;

import java.util.Set;

import javax.validation.constraints.NotNull;

import com.sequenceiq.environment.experience.common.responses.DeleteCommonExperienceWorkspaceResponse;

public interface CommonExperienceApi {

    @NotNull Set<String> getWorkspaceNamesConnectedToEnv(String experienceBasePath, String environmentCrn);

    @NotNull DeleteCommonExperienceWorkspaceResponse deleteWorkspaceForEnvironment(String experienceBasePath, String environmentCrn);

}
