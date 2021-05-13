package com.sequenceiq.environment.experience.api;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import com.sequenceiq.environment.experience.liftie.responses.DeleteClusterResponse;
import com.sequenceiq.environment.experience.liftie.responses.ListClustersResponse;

public interface LiftieApi {

    @NotNull ListClustersResponse listClusters(@NotNull String env, @NotNull String tenant, @Nullable String workload, @Nullable Integer page);

    @NotNull DeleteClusterResponse deleteCluster(@NotNull String clusterId);

    @Nullable String getPolicy(String cloudPlatform);

}
