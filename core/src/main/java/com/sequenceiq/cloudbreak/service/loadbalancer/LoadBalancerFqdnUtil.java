package com.sequenceiq.cloudbreak.service.loadbalancer;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.LoadBalancer;
import com.sequenceiq.cloudbreak.service.publicendpoint.GatewayPublicEndpointManagementService;
import com.sequenceiq.cloudbreak.service.stack.LoadBalancerPersistenceService;
import com.sequenceiq.common.api.type.LoadBalancerType;

@Service
public class LoadBalancerFqdnUtil {

    @Inject
    private LoadBalancerPersistenceService loadBalancerPersistenceService;

    @Inject
    private GatewayPublicEndpointManagementService gatewayPublicEndpointManagementService;

    /*
     * Favors public, then gateway_private over private
     */
    public String getLoadBalancerUserFacingFQDN(Long stackId) {
        Set<LoadBalancer> loadBalancers = loadBalancerPersistenceService.findByStackId(stackId).stream()
                .filter(lb -> StringUtils.isNotBlank(lb.getDns()) ||
                        StringUtils.isNotBlank(lb.getIp()) || StringUtils.isNotBlank(lb.getFqdn()))
                .collect(Collectors.toSet());

        return findLbNameForType(loadBalancers, LoadBalancerType.PUBLIC)
                .orElse(findLbNameForType(loadBalancers, LoadBalancerType.GATEWAY_PRIVATE)
                        .orElseGet(() -> findAnyLbNameOrNull(loadBalancers)));
    }

    public Set<LoadBalancer> getLoadBalancersForStack(Long stackId) {
        return loadBalancerPersistenceService.findByStackId(stackId);
    }

    private Optional<String> findLbNameForType(Set<LoadBalancer> loadBalancers, LoadBalancerType loadBalancerType) {
        return loadBalancers.stream()
                .filter(lb -> loadBalancerType.equals(lb.getType()))
                .findAny().map(this::getBestAddressable);
    }

    private String findAnyLbNameOrNull(Set<LoadBalancer> loadBalancers) {
        return loadBalancers.stream().findAny()
                .map(this::getBestAddressable).orElse(null);
    }

    private String getBestAddressable(LoadBalancer lb) {
        if (gatewayPublicEndpointManagementService.isPemEnabled() && StringUtils.isNotBlank(lb.getFqdn())) {
            return lb.getFqdn();
        }
        if (StringUtils.isNotBlank(lb.getDns())) {
            return lb.getDns();
        }
        return lb.getIp();
    }
}
