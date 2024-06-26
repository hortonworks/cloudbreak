package com.sequenceiq.cloudbreak.init.blueprint;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "cb.blueprint.cm.gov")
public class ExclusionListProperties {

    private Map<String, String> exclusions;

    private final Map<String, Set<String>> exclusionParsedMap = new HashMap<>();

    @PostConstruct
    public void parse() {
        for (Map.Entry<String, String> entry : exclusions.entrySet()) {
            exclusionParsedMap.put(entry.getKey(),
                    Arrays.stream(entry.getValue().split(",")).collect(Collectors.toSet()));
        }
    }

    public Map<String, String> getExclusions() {
        return exclusions;
    }

    public void setExclusions(Map<String, String> exclusions) {
        this.exclusions = exclusions;
    }

    public boolean doesVersionFilterExist(String stackVersion) {
        return exclusionParsedMap.containsKey(stackVersion);
    }

    public boolean isBlueprintExcluded(String stackVersion, String bpName) {
        if (doesVersionFilterExist(stackVersion)) {
            return getByStackVersion(stackVersion).contains(bpName);
        } else {
            return getDefaultList().contains(bpName);
        }
    }

    private Set<String> getDefaultList() {
        return getByStackVersion("default");
    }

    private Set<String> getByStackVersion(String stackVersion) {
        return exclusionParsedMap.getOrDefault(stackVersion, Set.of());
    }

}
