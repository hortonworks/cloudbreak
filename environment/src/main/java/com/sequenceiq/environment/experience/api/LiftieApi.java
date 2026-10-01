package com.sequenceiq.environment.experience.api;

import java.util.Map;
import java.util.Set;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;

import com.sequenceiq.environment.experience.liftie.responses.DeleteClusterResponse;
import com.sequenceiq.environment.experience.liftie.responses.ListClustersResponse;
import com.sequenceiq.environment.experience.policy.response.ExperiencePolicyResponse;

public interface LiftieApi {

    @NotNull ListClustersResponse listClusters(@NotNull String env, @NotNull String tenant, @Nullable String workload, @Nullable Integer page);

    @NotNull DeleteClusterResponse deleteCluster(@NotNull String clusterId);

    @Nullable ExperiencePolicyResponse getPolicy(String cloudPlatform);

    void distributeEnvironmentTags(String experienceBasePath, String environmentCrn, Map<String, String> tags);

    void deleteEnvironmentTags(String experienceBasePath, String environmentCrn, Set<String> tagKeys);

}
