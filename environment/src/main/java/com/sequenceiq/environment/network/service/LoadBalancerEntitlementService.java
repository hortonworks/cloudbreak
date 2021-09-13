package com.sequenceiq.environment.network.service;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.type.CloudConstants;
import com.sequenceiq.common.api.type.PublicEndpointAccessGateway;

@Component
public class LoadBalancerEntitlementService {

    @Inject
    private EntitlementService entitlementService;

    public void validateNetworkForEndpointGateway(String cloudPlatform, String environmentName,
            PublicEndpointAccessGateway endpointAccessGateway) {
        if (!isEndpointGatewayEntitlementEnabledForPlatform(cloudPlatform)) {
            if (PublicEndpointAccessGateway.ENABLED.equals(endpointAccessGateway)) {
                throw new BadRequestException(String.format("Environment %s cannot be created. Public Endpoint Gateway is not " +
                    "supported on %s platform, or the Endpoint Gateway entitlement is missing.", environmentName, cloudPlatform));
            }
        }
    }

    private boolean isEndpointGatewayEntitlementEnabledForPlatform(String cloudPlatform) {
        return CloudConstants.AWS.equals(cloudPlatform) ||
            (CloudConstants.AZURE.equals(cloudPlatform) &&
                entitlementService.azureEndpointGatewayEnabled(ThreadBasedUserCrnProvider.getAccountId())) ||
            (CloudConstants.GCP.equals(cloudPlatform) &&
                entitlementService.gcpEndpointGatewayEnabled(ThreadBasedUserCrnProvider.getAccountId()));
    }
}
