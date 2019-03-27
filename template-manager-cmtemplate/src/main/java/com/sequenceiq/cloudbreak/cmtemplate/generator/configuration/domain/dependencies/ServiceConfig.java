package com.sequenceiq.cloudbreak.cmtemplate.generator.configuration.domain.dependencies;

import java.util.HashSet;
import java.util.Set;

public class ServiceConfig {

    private String name;

    private String displayName;

    private Set<String> dependencies = new HashSet<>();

    private Set<ComponentConfig> components = new HashSet<>();

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

    public Set<String> getDependencies() {
        return dependencies;
    }

    public void setDependencies(Set<String> dependencies) {
        this.dependencies = dependencies;
    }

    public Set<ComponentConfig> getComponents() {
        return components;
    }

    public void setComponents(Set<ComponentConfig> components) {
        this.components = components;
    }

}
