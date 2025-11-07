package com.sequenceiq.freeipa.service.crossrealm;

import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.LoadBalancer;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.loadbalancer.FreeIpaLoadBalancerService;

@Component
public class StackHelper {
    @Inject
    private FreeIpaLoadBalancerService freeIpaLoadBalancerService;

    public List<String> getServerIps(Stack stack) {
        Optional<LoadBalancer> loadBalancer = freeIpaLoadBalancerService.findByStackId(stack.getId());
        return loadBalancer
                .map(balancer -> balancer.getIp().stream().toList())
                .orElseGet(() -> stack.getNotDeletedInstanceMetaDataSet().stream()
                        .map(InstanceMetaData::getPrivateIp)
                        .toList());
    }
}
