package com.sequenceiq.cloudbreak.service.loadbalancer;

import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AWS;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.common.api.type.LoadBalancerCreation;
import com.sequenceiq.common.api.type.PublicEndpointAccessGateway;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;

@Service
public class LoadBalancerEnabler {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoadBalancerEnabler.class);

    @Value("${cb.loadBalancer.supportedPlatforms:}")
    private String supportedPlatforms;

    @Inject
    private EntitlementService entitlementService;

    private Set<String> supportedPlatformSet;

    @PostConstruct
    public void init() {
        supportedPlatformSet = supportedPlatforms == null
                ? Set.of()
                : Arrays.stream(supportedPlatforms.split(",")).collect(Collectors.toSet());
    }

    public boolean isLoadBalancerEnabled(StackType type, String cloudPlatform, DetailedEnvironmentResponse environment, boolean enableLoadBalancerOnStack) {
        return !type.equals(StackType.TEMPLATE) &&
                supportedPlatformSet.contains(cloudPlatform) &&
                !isLoadBalancerDisabled(environment) &&
                (enableLoadBalancerOnStack || isLoadBalancerEnabledForDatalake(type, environment) || isLoadBalancerEnabledForDatahub(type, environment));
    }

    public boolean isEndpointGatewayEnabled(String accountId, EnvironmentNetworkResponse network) {
        boolean result = network != null && (network.getPublicEndpointAccessGateway() == PublicEndpointAccessGateway.ENABLED ||
                entitlementService.isTargetingSubnetsForEndpointAccessGatewayEnabled(accountId) &&
                        CollectionUtils.isNotEmpty(network.getEndpointGatewaySubnetIds()));
        if (result) {
            LOGGER.debug("Endpoint access gateway is enabled. A load balancer will be created with{} public IP.",
                    network.getPublicEndpointAccessGateway() == PublicEndpointAccessGateway.ENABLED ? "" : "out");
        } else {
            LOGGER.debug("Endpoint access gateway is disabled.");
        }
        return result;
    }

    private boolean isLoadBalancerDisabled(DetailedEnvironmentResponse environment) {
        return environment != null && environment.getNetwork() != null &&
                LoadBalancerCreation.DISABLED.equals(environment.getNetwork().getLoadBalancerCreation());
    }

    private boolean isLoadBalancerEnabledForDatalake(StackType type, DetailedEnvironmentResponse environment) {
        return StackType.DATALAKE.equals(type) && environment != null &&
                (isDatalakeLoadBalancerEntitlementEnabled(environment.getAccountId(), environment.getCloudPlatform()) ||
                        !isLoadBalancerEntitlementRequiredForCloudProvider(environment.getCloudPlatform()) ||
                        isEndpointGatewayEnabled(environment.getAccountId(), environment.getNetwork()));
    }

    private boolean isLoadBalancerEnabledForDatahub(StackType type, DetailedEnvironmentResponse environment) {
        return StackType.WORKLOAD.equals(type) && environment != null &&
                isEndpointGatewayEnabled(environment.getAccountId(), environment.getNetwork());
    }

    private boolean isDatalakeLoadBalancerEntitlementEnabled(String accountId, String cloudPlatform) {
            return entitlementService.datalakeLoadBalancerEnabled(accountId);
    }

    private boolean isLoadBalancerEntitlementRequiredForCloudProvider(String cloudPlatform) {
        return !AWS.equalsIgnoreCase(cloudPlatform);
    }

}
