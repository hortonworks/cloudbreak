package com.sequenceiq.cloudbreak.clusterproxy;

import java.util.Set;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ClusterProxyEnablementService {

    @Value("${clusterProxy.disabledPlatforms}")
    private Set<String> clusterProxyDisabledPlatforms;

    @Inject
    private ClusterProxyConfiguration clusterProxyConfiguration;

    public boolean isClusterProxyApplicable(String cloudPlatform) {
        return clusterProxyConfiguration.isClusterProxyIntegrationEnabled() && !clusterProxyDisabledPlatforms.contains(cloudPlatform);
    }

}
