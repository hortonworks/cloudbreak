package com.sequenceiq.freeipa.service.loadbalancer;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "freeipa.loadbalancer")
public class LoadBalancerTargets {

    private Map<String, String> targets = new HashMap<>();

    public Map<String, String> getTargets() {
        return this.targets;
    }
}
