package com.sequenceiq.environment.experience.liftie;

import com.sequenceiq.environment.experience.liftie.responses.ListClustersResponse;

import javax.validation.constraints.NotNull;

public interface LiftieApi {

    ListClustersResponse listClusters(@NotNull String env, @NotNull String tenant, String workloads, Integer page);

}
