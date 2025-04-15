package com.sequenceiq.freeipa.service.freeipa.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public record FreeIpaLbConfigView(boolean enabled, String endpoint, String fqdn, Set<String> ips) {

    private static final int PARAMETERS_COUNT = 4;

    public FreeIpaLbConfigView(String endpoint, String fqdn, Set<String> ips) {
        this(true, endpoint, fqdn, ips);
    }

    public FreeIpaLbConfigView() {
        this(false, null, null, null);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> result = new HashMap<>(PARAMETERS_COUNT);
        result.put("enabled", enabled);
        Optional.ofNullable(endpoint).ifPresent(endpoint -> result.put("endpoint", endpoint));
        Optional.ofNullable(fqdn).ifPresent(fqdn -> result.put("fqdn", fqdn));
        Optional.ofNullable(ips).ifPresent(ips -> result.put("ips", ips));
        return result;
    }
}
