package com.sequenceiq.common.api.cloudstorage.query;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ConfigQueryEntries {

    private final Set<ConfigQueryEntry> entries;

    public ConfigQueryEntries() {
        entries = new HashSet<>();
    }

    public ConfigQueryEntries(Set<ConfigQueryEntry> entries) {
        this.entries = entries;
    }

    public Set<ConfigQueryEntry> getEntries() {
        return entries
                .stream()
                .map(ConfigQueryEntry::copy)
                .collect(Collectors.toSet());
    }

}
