package com.sequenceiq.environment.experience.liftie;

import javax.validation.constraints.NotNull;

import com.sequenceiq.environment.experience.liftie.responses.ListClustersResponse;

public interface LiftieApi {

    ListClustersResponse listClusters(@NotNull String env, @NotNull String tenant, String workloads, Integer page);

}
