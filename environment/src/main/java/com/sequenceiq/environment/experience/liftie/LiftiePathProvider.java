package com.sequenceiq.environment.experience.liftie;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.environment.experience.config.ExperiencePathConfig;

@Component
public class LiftiePathProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(LiftiePathProvider.class);

    private final String basePath;

    private final String policyPath;

    private ExperiencePathConfig pathConfig;

    public LiftiePathProvider(ExperiencePathConfig pathConfig, @Value("${environment.experience.liftie.address}") String liftieApi,
            @Value("${environment.experience.liftie.pathInfix}") String pathInfix,
            @Value("${environment.experience.liftie.policyPath}") String policyPath) {
        this.basePath = liftieApi + pathInfix;
        this.policyPath = policyPath;
        this.pathConfig = pathConfig;
        LOGGER.debug("Liftie address has been set to: {}", basePath);
    }

    public String getPathToPolicyEndpoint(String provider) {
        return (basePath + policyPath).replace(pathConfig.getToReplace().get("cloudProvider"), provider);
    }

    public String getPathToClustersEndpoint() {
        return basePath + "/cluster";
    }

    public String getPathToClusterEndpoint(String clusterId) {
        return String.format("%s/%s", getPathToClustersEndpoint(), clusterId);
    }

}
