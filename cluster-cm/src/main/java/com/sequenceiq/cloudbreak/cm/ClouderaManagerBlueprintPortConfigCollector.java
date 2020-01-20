package com.sequenceiq.cloudbreak.cm;

import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.service.ExposedServiceCollector;
import com.sequenceiq.cloudbreak.domain.Blueprint;

@Component
public class ClouderaManagerBlueprintPortConfigCollector {

    @Inject
    private ExposedServiceCollector exposedServiceCollector;

    public Map<String, Integer> getServicePorts(Blueprint blueprint, boolean tls) {
        return exposedServiceCollector.getAllServicePorts(tls);
    }
}
