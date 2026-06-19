package com.sequenceiq.environment.experience.liftie;

import org.apache.commons.lang3.StringUtils;
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

    private final String environmentTagsPath;

    private final ExperiencePathConfig pathConfig;

    public LiftiePathProvider(ExperiencePathConfig pathConfig, @Value("${environment.experience.liftie.address}") String liftieApi,
            @Value("${environment.experience.liftie.pathInfix}") String pathInfix,
            @Value("${environment.experience.liftie.policyPath}") String policyPath,
            @Value("${environment.experience.environmentTagsPath:}") String environmentTagsPath) {
        this.basePath = liftieApi + pathInfix;
        this.policyPath = policyPath;
        this.pathConfig = pathConfig;
        this.environmentTagsPath = environmentTagsPath;
        LOGGER.debug("Liftie address has been set to: {}, environment tags distribution {}",
                basePath, isEnvironmentTagsDistributionDisabled() ? "disabled" : "enabled");
    }

    public String getPathToPolicyEndpoint(String provider) {
        String path = (basePath + policyPath).replace(pathConfig.getToReplace().get("cloudProvider"), provider);
        LOGGER.info("Path has created to liftie for policy fetching: {}", path);
        return path;
    }

    public String getPathToEnvironmentTagsEndpoint() {
        String path = basePath + environmentTagsPath;
        LOGGER.info("Path has created to liftie for environment tags distribution: {}", path);
        return path;
    }

    public boolean isPolicyFetchDisabled() {
        return StringUtils.isEmpty(policyPath);
    }

    public boolean isEnvironmentTagsDistributionDisabled() {
        return StringUtils.isEmpty(environmentTagsPath);
    }

    public String getPathToClustersEndpoint() {
        String path = basePath + "/cluster";
        LOGGER.info("Path for Liftie's clusters endpoint: {}", path);
        return path;
    }

    public String getPathToClusterEndpoint(String clusterId) {
        String path = String.format("%s/%s", getPathToClustersEndpoint(), clusterId);
        LOGGER.info("Path for Liftie's clusters endpoint with the cluster id of [{}]: {}", clusterId, path);
        return path;
    }

}
