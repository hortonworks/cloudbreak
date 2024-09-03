package com.sequenceiq.cloudbreak.service.upgrade;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "cb.upgrade")
public class ServicesToRemoveBeforeUpgrade {

    private Map<String, String> servicesToRemove = new HashMap<>();

    public Map<String, String> getServicesToRemove() {
        return servicesToRemove;
    }
}
