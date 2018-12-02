package com.sequenceiq.cloudbreak.domain.stack.cluster;

import java.util.HashSet;
import java.util.Set;

public class ServiceDescriptorDefinition {
    private String serviceName;

    private Set<String> blueprintParamKeys = new HashSet<>();

    private Set<String> blueprintSecretParamKeys = new HashSet<>();

    private Set<String> componentHosts = new HashSet<>();

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public Set<String> getBlueprintParamKeys() {
        return blueprintParamKeys;
    }

    public void setBlueprintParamKeys(Set<String> blueprintParamKeys) {
        this.blueprintParamKeys = blueprintParamKeys;
    }

    public Set<String> getBlueprintSecretParamKeys() {
        return blueprintSecretParamKeys;
    }

    public void setBlueprintSecretParamKeys(Set<String> blueprintSecretParamKeys) {
        this.blueprintSecretParamKeys = blueprintSecretParamKeys;
    }

    public Set<String> getComponentHosts() {
        return componentHosts;
    }

    public void setComponentHosts(Set<String> componentHosts) {
        this.componentHosts = componentHosts;
    }
}
