package com.sequenceiq.cloudbreak.validation.externaldatabase;


import java.util.HashSet;
import java.util.Set;

public class SupportedExternalDatabaseServiceEntry {

    private String name;

    private String displayName;

    private Set<SupportedDatabaseEntry> databases = new HashSet<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Set<SupportedDatabaseEntry> getDatabases() {
        return databases;
    }

    public void setDatabases(Set<SupportedDatabaseEntry> databases) {
        this.databases = databases;
    }
}
