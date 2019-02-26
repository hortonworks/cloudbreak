package com.sequenceiq.cloudbreak.template.filesystem.query;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ConfigQueryEntries {

    private final Set<ConfigQueryEntry> entries = new HashSet<>();

    public Set<ConfigQueryEntry> getEntries() {
        return entries
                .stream()
                .map(ConfigQueryEntry::copy)
                .collect(Collectors.toSet());
    }
}
