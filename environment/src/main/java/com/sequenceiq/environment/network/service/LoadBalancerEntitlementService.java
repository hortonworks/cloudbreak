package com.sequenceiq.environment.network.service;

import java.util.Set;

import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.type.CloudConstants;

@Component
public class LoadBalancerEntitlementService {

    @Inject
    private EntitlementService entitlementService;

    public void validateNetworkForEndpointGateway(String cloudPlatform, String environmentName, Set<String> endpointGatewaySubnetIds) {
        if (!isEndpointGatewayEntitlementEnabledForPlatform(cloudPlatform)
                && CollectionUtils.isNotEmpty(endpointGatewaySubnetIds)) {
            throw new BadRequestException(String.format("Environment %s cannot be created. Endpoint Gateway is not " +
                    "supported on %s platform, or the Endpoint Gateway entitlement is missing.", environmentName, cloudPlatform));
        }
    }

    private boolean isEndpointGatewayEntitlementEnabledForPlatform(String cloudPlatform) {
        return CloudConstants.AWS.equals(cloudPlatform) ||
                CloudConstants.AZURE.equals(cloudPlatform) &&
                        entitlementService.azureEndpointGatewayEnabled(ThreadBasedUserCrnProvider.getAccountId()) ||
                CloudConstants.GCP.equals(cloudPlatform) &&
                        entitlementService.gcpEndpointGatewayEnabled(ThreadBasedUserCrnProvider.getAccountId());
    }
}
