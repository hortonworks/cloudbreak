package com.sequenceiq.cloudbreak.service.loadbalancer;

import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AZURE;

import java.util.Map;

import jakarta.inject.Inject;

import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.network.SubnetType;
import com.sequenceiq.common.api.type.LoadBalancerType;
import com.sequenceiq.common.api.type.PublicEndpointAccessGateway;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@Component
public class LoadBalancerTypeDeterminer {

    @Inject
    private EntitlementService entitlementService;

    public LoadBalancerType getType(DetailedEnvironmentResponse environment) {
        return isPrivateEndpointGatewayNetwork(environment) ? LoadBalancerType.GATEWAY_PRIVATE : LoadBalancerType.PUBLIC;
    }

    private boolean isPrivateEndpointGatewayNetwork(DetailedEnvironmentResponse environment) {
        return entitlementService.isTargetingSubnetsForEndpointAccessGatewayEnabled(environment.getAccountId())
                && isSubnetPrivateIfApplicable(environment)
                && PublicEndpointAccessGateway.DISABLED == environment.getNetwork().getPublicEndpointAccessGateway();
    }

    /**
     * In case of Azure, private subnet as such has no meaning, and SubnetType is null
     *
     * @param environment {@link DetailedEnvironmentResponse}
     * @return true if private subnets are present for endpoint gateway subnet, however the "privateness" is not checked with Azure
     */
    private boolean isSubnetPrivateIfApplicable(DetailedEnvironmentResponse environment) {
        boolean azure = AZURE.equalsIgnoreCase(environment.getCloudPlatform());
        Map<String, CloudSubnet> gatewayEndpointSubnetMetas = environment.getNetwork().getGatewayEndpointSubnetMetas();
        return azure && MapUtils.isNotEmpty(gatewayEndpointSubnetMetas)
                || !azure && MapUtils.isNotEmpty(gatewayEndpointSubnetMetas) && gatewayEndpointSubnetMetas.entrySet().stream()
                        .allMatch(e -> e.getValue().getType() == SubnetType.PRIVATE);
    }

}
