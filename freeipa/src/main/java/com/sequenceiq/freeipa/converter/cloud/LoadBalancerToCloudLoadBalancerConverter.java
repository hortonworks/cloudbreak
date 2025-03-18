package com.sequenceiq.freeipa.converter.cloud;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancer;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.HealthProbeParameters;
import com.sequenceiq.cloudbreak.cloud.model.NetworkProtocol;
import com.sequenceiq.cloudbreak.cloud.model.TargetGroupPortPair;
import com.sequenceiq.common.api.type.LoadBalancerType;
import com.sequenceiq.freeipa.entity.LoadBalancer;
import com.sequenceiq.freeipa.entity.TargetGroup;
import com.sequenceiq.freeipa.service.loadbalancer.FreeIpaLoadBalancerService;

@Service
public class LoadBalancerToCloudLoadBalancerConverter {

    @Value("${freeipa.loadbalancer.health-check.path}")
    private String healthCheckPath;

    @Value("${freeipa.loadbalancer.health-check.port}")
    private int healthCheckPort;

    @Value("${freeipa.loadbalancer.health-check.protocol}")
    private String healthCheckProtocol;

    @Value("${freeipa.loadbalancer.health-check.interval}")
    private int healthCheckInterval;

    @Value("${freeipa.loadbalancer.health-check.probeDownThreshold}")
    private int healthCheckProbeDownThreshold;

    @Inject
    private FreeIpaLoadBalancerService freeIpaLoadBalancerService;

    public List<CloudLoadBalancer> convertLoadBalancer(Long stackId, Set<Group> instanceGroups) {
        Optional<LoadBalancer> loadBalancer = freeIpaLoadBalancerService.findByStackId(stackId);
        if (loadBalancer.isPresent()) {
            CloudLoadBalancer cloudLoadBalancer = new CloudLoadBalancer(LoadBalancerType.PRIVATE);
            HealthProbeParameters healthProbeParameters = new HealthProbeParameters(healthCheckPath, healthCheckPort,
                    NetworkProtocol.valueOf(healthCheckProtocol), healthCheckInterval, healthCheckProbeDownThreshold);
            loadBalancer.get().getTargetGroups().forEach(targetGroup ->
                    cloudLoadBalancer.addPortToTargetGroupMapping(
                            new TargetGroupPortPair(targetGroup.getTrafficPort(), getTrafficProtocol(targetGroup), healthProbeParameters), instanceGroups));
            return Collections.singletonList(cloudLoadBalancer);
        } else {
            return Collections.emptyList();
        }
    }

    private NetworkProtocol getTrafficProtocol(TargetGroup targetGroup) {
        return NetworkProtocol.valueOf(targetGroup.getProtocol());
    }
}
