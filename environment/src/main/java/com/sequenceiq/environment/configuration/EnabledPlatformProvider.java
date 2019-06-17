package com.sequenceiq.environment.configuration;

import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class EnabledPlatformProvider {

    private static final Set<String> DEFAULT_ENABLED_PLATFORMS = Set.of("AWS", "AZURE", "MOCK");

    @Value("${environment.enabled.platforms:}")
    private String enabledPlatforms;

    public Set<String> enabledPlatforms() {
        if (enabledPlatforms.isEmpty()) {
            return DEFAULT_ENABLED_PLATFORMS;
        }
        return Set.of(enabledPlatforms.split(","));
    }
}
