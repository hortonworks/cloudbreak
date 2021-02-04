package com.sequenceiq.environment.experience.api;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import com.sequenceiq.environment.experience.liftie.responses.DeleteClusterResponse;
import com.sequenceiq.environment.experience.liftie.responses.ListClustersResponse;

public interface LiftieApi {

    @NotNull ListClustersResponse listPagedClustersWithWorkloadFilter(@NotNull String env, @NotNull String tenant, @Nullable Integer page,
            @Nullable String workload);

    @NotNull ListClustersResponse listClusters(@NotNull String env, @NotNull String tenant, @Nullable Integer page, @Nullable String workload);

    @NotNull ListClustersResponse listClustersWithWorkloadFilter(@NotNull String env, @NotNull String tenant, @Nullable String workload);

    @NotNull ListClustersResponse listPagedClusters(@NotNull String env, @NotNull String tenant, @Nullable Integer page);

    @NotNull DeleteClusterResponse deleteCluster(@NotNull String clusterId);

}
