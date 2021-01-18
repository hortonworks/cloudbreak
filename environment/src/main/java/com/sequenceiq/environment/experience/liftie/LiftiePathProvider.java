package com.sequenceiq.environment.experience.liftie;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LiftiePathProvider {

    private final String liftieBasePath;

    public LiftiePathProvider(@Value("${experience.scan.liftie.api.port}") String liftiePort,
            @Value("${experience.scan.liftie.api.address}") String liftieAddress,
            @Value("${experience.scan.protocol}") String liftieProtocol) {
        this.liftieBasePath = String.format("%s://%s:%s/liftie/api/v1", liftieProtocol, liftieAddress, liftiePort);
    }

    public String getPathToClustersEndpoint() {
        return liftieBasePath + "/cluster";
    }

    public String getPathToClusterEndpoint(String clusterId) {
        return String.format("%s/%s", getPathToClustersEndpoint(), clusterId);
    }

}
