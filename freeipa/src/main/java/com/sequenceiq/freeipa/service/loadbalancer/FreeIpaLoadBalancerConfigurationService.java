package com.sequenceiq.freeipa.service.loadbalancer;

import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancerMetadata;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.LoadBalancer;
import com.sequenceiq.freeipa.entity.TargetGroup;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaService;

@Service
public class FreeIpaLoadBalancerConfigurationService {

    private static final String LB_DOMAIN_NAME_SUFFIX = "-lb";

    @Inject
    private LoadBalancerTargets loadBalancerTargets;

    @Inject
    private FreeIpaService freeIpaService;

    public LoadBalancer createLoadBalancerConfiguration(Long stackId, String name) {
        FreeIpa freeIpa = freeIpaService.findByStackId(stackId);
        String endpoint = name + LB_DOMAIN_NAME_SUFFIX;
        LoadBalancer loadBalancer = new LoadBalancer();
        loadBalancer.setStackId(stackId);
        loadBalancer.setTargetGroups(createTargetGroups(loadBalancer));
        loadBalancer.setEndpoint(endpoint);
        loadBalancer.setFqdn(endpoint + "." + freeIpa.getDomain());
        return loadBalancer;
    }

    public LoadBalancer extendConfigurationWithMetadata(LoadBalancer loadBalancer, CloudLoadBalancerMetadata metadata) {
        loadBalancer.setDns(metadata.getCloudDns());
        loadBalancer.setResourceId(metadata.getName());
        loadBalancer.setIp(metadata.getIp());
        loadBalancer.setHostedZoneId(metadata.getHostedZoneId());
        return loadBalancer;
    }

    private Set<TargetGroup> createTargetGroups(LoadBalancer loadBalancer) {
        return loadBalancerTargets.getTargets().entrySet().stream()
                .map(entry -> createTargetGroup(entry.getKey(), entry.getValue(), loadBalancer))
                .collect(Collectors.toSet());
    }

    private TargetGroup createTargetGroup(String targetPort, String protocol, LoadBalancer loadBalancer) {
        TargetGroup targetGroup = new TargetGroup();
        targetGroup.setTrafficPort(Integer.parseInt(targetPort));
        targetGroup.setProtocol(protocol);
        targetGroup.setLoadBalancer(loadBalancer);
        return targetGroup;
    }
}
