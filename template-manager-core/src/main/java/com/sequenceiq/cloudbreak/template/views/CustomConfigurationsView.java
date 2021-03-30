package com.sequenceiq.cloudbreak.template.views;

import java.util.Set;

public class CustomConfigurationsView {

    private String name;

    private String crn;

    private String runtimeVersion;

    private Set<CustomConfigurationPropertyView> configurations;

    public CustomConfigurationsView(String name, String crn, String runtimeVersion, Set<CustomConfigurationPropertyView> configurations) {
        this.name = name;
        this.crn = crn;
        this.runtimeVersion = runtimeVersion;
        this.configurations = configurations;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCrn() {
        return crn;
    }

    public void setCrn(String crn) {
        this.crn = crn;
    }

    public String getRuntimeVersion() {
        return runtimeVersion;
    }

    public void setRuntimeVersion(String runtimeVersion) {
        this.runtimeVersion = runtimeVersion;
    }

    public Set<CustomConfigurationPropertyView> getConfigurations() {
        return configurations;
    }

    public void setConfigurations(Set<CustomConfigurationPropertyView> configurations) {
        this.configurations = configurations;
    }

}
