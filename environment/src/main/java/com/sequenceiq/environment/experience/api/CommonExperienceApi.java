package com.sequenceiq.environment.experience.api;

import java.util.Set;

import jakarta.validation.constraints.NotNull;

import com.sequenceiq.environment.experience.common.responses.CpInternalCluster;
import com.sequenceiq.environment.experience.policy.response.ExperiencePolicyResponse;

public interface CommonExperienceApi {

    @NotNull Set<CpInternalCluster> getExperienceClustersConnectedToEnv(String experienceBasePath, String environmentCrn);

    void deleteWorkspaceForEnvironment(String experienceBasePath, String environmentCrn, boolean force);

    @NotNull ExperiencePolicyResponse collectPolicy(String experienceBasePath, String cloudPlatform);

}
