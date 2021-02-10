package com.sequenceiq.environment.experience.liftie;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LiftiePathProvider {

    private final String liftieBasePath;

    public LiftiePathProvider(@Value("${environment.experience.liftie.address}") String liftieApi) {
        this.liftieBasePath = String.format("%s/liftie/api/v1", liftieApi);
    }

    public String getPathToClustersEndpoint() {
        return liftieBasePath + "/cluster";
    }

    public String getPathToClusterEndpoint(String clusterId) {
        return String.format("%s/%s", getPathToClustersEndpoint(), clusterId);
    }

}
