package com.sequenceiq.cloudbreak.cmtemplate.generator.configuration.domain.dependencies;

import java.util.HashSet;
import java.util.Set;

public class ComponentConfig {

    private String name;

    private Set<String> groups = new HashSet<>();

    private boolean base;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<String> getGroups() {
        return groups;
    }

    public void setGroups(Set<String> groups) {
        this.groups = groups;
    }

    public boolean isBase() {
        return base;
    }

    public void setBase(boolean base) {
        this.base = base;
    }
}
