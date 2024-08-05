package com.sequenceiq.freeipa.service.loadbalancer;

import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancerMetadata;
import com.sequenceiq.freeipa.entity.LoadBalancer;
import com.sequenceiq.freeipa.entity.TargetGroup;

@Service
public class FreeIpaLoadBalancerConfigurationService {

    @Inject
    private LoadBalancerTargets loadBalancerTargets;

    public LoadBalancer createLoadBalancerConfiguration(Long stackId) {
        LoadBalancer loadBalancer = new LoadBalancer();
        loadBalancer.setStackId(stackId);
        loadBalancer.setTargetGroups(createTargetGroups(loadBalancer));
        return loadBalancer;
    }

    public LoadBalancer extendConfigurationWithMetadata(LoadBalancer loadBalancer, CloudLoadBalancerMetadata metadata) {
        LoadBalancer extended = loadBalancer;
        extended.setDns(metadata.getCloudDns());
        extended.setResourceId(metadata.getName());
        extended.setIp(metadata.getIp());
        extended.setHostedZoneId(metadata.getHostedZoneId());
        return extended;
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
