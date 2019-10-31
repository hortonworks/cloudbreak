package com.sequenceiq.freeipa.service.config;

import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.security.InternalCrnBuilder;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyConfiguration;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.stack.StackService;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class FmsClusterProxyEnablement {

    private static final String INTERNAL_ACTOR_CRN = new InternalCrnBuilder(Crn.Service.IAM).getInternalCrnForServiceAsString();

    @Inject
    private ClusterProxyConfiguration clusterProxyConfiguration;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private StackService stackService;

    public boolean isEnabled(Stack stack) {
        return clusterProxyConfiguration.isClusterProxyIntegrationEnabled() && hasEntitlement(stack.getAccountId());
    }

    private boolean hasEntitlement(String accountId) {
        return entitlementService.fmsClusterProxyEnabled(INTERNAL_ACTOR_CRN, accountId);
    }
}
