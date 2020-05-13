package com.sequenceiq.environment.environment.experience.liftie;

import com.sequenceiq.environment.environment.experience.liftie.responses.ListClustersResponse;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;

@Component
public interface LiftieApi {

    ListClustersResponse listClusters(@NotNull String env, @NotNull String tenant, String workloads, Integer page);

}
