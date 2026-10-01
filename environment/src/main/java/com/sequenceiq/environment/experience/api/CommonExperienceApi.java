package com.sequenceiq.environment.experience.api;

import java.util.Map;
import java.util.Set;

import jakarta.validation.constraints.NotNull;

import com.sequenceiq.environment.experience.common.responses.CpInternalCluster;
import com.sequenceiq.environment.experience.policy.response.ExperiencePolicyResponse;

public interface CommonExperienceApi {

    String TAG_KEY_QUERY_PARAM = "tagKey";

    @NotNull Set<CpInternalCluster> getExperienceClustersConnectedToEnv(String experienceBasePath, String environmentCrn);

    void deleteWorkspaceForEnvironment(String experienceBasePath, String environmentCrn, boolean force);

    @NotNull ExperiencePolicyResponse collectPolicy(String experienceBasePath, String cloudPlatform);

    void distributeEnvironmentTags(String experienceBasePath, String environmentCrn, Map<String, String> tags);

    void deleteEnvironmentTags(String experienceBasePath, String environmentCrn, Set<String> tagKeys);

}
