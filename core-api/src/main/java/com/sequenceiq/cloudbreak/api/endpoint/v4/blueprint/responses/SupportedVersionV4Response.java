package com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses;

import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.common.model.JsonEntity;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SupportedVersionV4Response implements JsonEntity {

    private Set<SupportedServiceV4Response> services = new TreeSet<>();

    private String version;

    private String type;

    public Set<SupportedServiceV4Response> getServices() {
        return services;
    }

    public void setServices(Collection<SupportedServiceV4Response> services) {
        this.services.addAll(services);
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
