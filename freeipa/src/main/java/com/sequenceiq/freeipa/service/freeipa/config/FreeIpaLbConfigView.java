package com.sequenceiq.freeipa.service.freeipa.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public record FreeIpaLbConfigView(boolean enabled, String endpoint, String fqdn) {

    private static final int PARAMETERS_COUNT = 3;

    public FreeIpaLbConfigView(String endpoint, String fqdn) {
        this(true, endpoint, fqdn);
    }

    public FreeIpaLbConfigView() {
        this(false, null, null);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> result = new HashMap<>(PARAMETERS_COUNT);
        result.put("enabled", enabled);
        Optional.ofNullable(endpoint).ifPresent(endpoint -> result.put("endpoint", endpoint));
        Optional.ofNullable(fqdn).ifPresent(fqdn -> result.put("fqdn", fqdn));
        return result;
    }
}
