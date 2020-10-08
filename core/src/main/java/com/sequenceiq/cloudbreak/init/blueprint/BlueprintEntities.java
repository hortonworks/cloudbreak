package com.sequenceiq.cloudbreak.init.blueprint;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "cb.blueprint.cm")
public class BlueprintEntities {

    private Map<String, String> defaults = new HashMap<>();

    public Map<String, String> getDefaults() {
        return this.defaults;
    }
}
