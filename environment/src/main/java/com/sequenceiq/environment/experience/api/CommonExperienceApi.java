package com.sequenceiq.environment.experience.api;

import java.util.Map;
import java.util.Set;

import javax.validation.constraints.NotNull;

import com.sequenceiq.environment.experience.common.responses.CpInternalCluster;
import com.sequenceiq.environment.experience.common.responses.DeleteCommonExperienceWorkspaceResponse;

public interface CommonExperienceApi {

    @NotNull Set<CpInternalCluster> getExperienceClustersConnectedToEnv(String experienceBasePath, String environmentCrn);

    @NotNull DeleteCommonExperienceWorkspaceResponse deleteWorkspaceForEnvironment(String experienceBasePath, String environmentCrn);

    @NotNull Map<String, String> collectPolicy(String experienceBasePath, String cloudPlatform);

}
