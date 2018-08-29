package com.sequenceiq.cloudbreak.template.filesystem.query;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ConfigQueryEntries {

    private Set<ConfigQueryEntry> entries = new HashSet<>();

    public Set<ConfigQueryEntry> getEntries() {
        return entries;
    }

    public void setEntries(Set<ConfigQueryEntry> entries) {
        this.entries = entries;
    }
}
