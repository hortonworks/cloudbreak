package com.sequenceiq.cloudbreak.structuredevent.event;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CustomConfigurationsDetails implements Serializable {
    private Long id;

    private String customConfigurationsName;

    private List<String> services;

    private List<String> roles;

    private String runtimeVersion;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCustomConfigurationsName() {
        return customConfigurationsName;
    }

    public void setCustomConfigurationsName(String customConfigurationsName) {
        this.customConfigurationsName = customConfigurationsName;
    }

    public List<String> getServices() {
        return services;
    }

    public void setServices(List<String> services) {
        this.services = services;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public String getRuntimeVersion() {
        return runtimeVersion;
    }

    public void setRuntimeVersion(String runtimeVersion) {
        this.runtimeVersion = runtimeVersion;
    }
}
