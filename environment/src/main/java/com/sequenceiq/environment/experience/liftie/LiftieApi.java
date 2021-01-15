package com.sequenceiq.environment.experience.liftie;

import javax.validation.constraints.NotNull;

import com.sequenceiq.environment.experience.liftie.responses.DeleteClusterResponse;
import com.sequenceiq.environment.experience.liftie.responses.ListClustersResponse;

public interface LiftieApi {

    ListClustersResponse listClusters(@NotNull String env, @NotNull String tenant, Integer page);

    DeleteClusterResponse deleteCluster(@NotNull String clusterId);

}
