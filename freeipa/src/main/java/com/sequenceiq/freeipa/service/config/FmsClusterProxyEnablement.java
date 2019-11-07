package com.sequenceiq.freeipa.service.config;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.security.InternalCrnBuilder;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyConfiguration;

@Component
public class FmsClusterProxyEnablement {

    private static final String INTERNAL_ACTOR_CRN = new InternalCrnBuilder(Crn.Service.IAM).getInternalCrnForServiceAsString();

    @Inject
    private ClusterProxyConfiguration clusterProxyConfiguration;

    public boolean isEnabled() {
        return clusterProxyConfiguration.isClusterProxyIntegrationEnabled();
    }
}
