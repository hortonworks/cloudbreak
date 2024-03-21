package com.sequenceiq.remoteenvironment.api.v1.controlplane.model.registration;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "PrivateControlPlaneRegistrationResponses")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PrivateControlPlaneRegistrationResponses {

    @Schema
    private Set<PrivateControlPlaneRegistrationResponse> items = new HashSet<>();

    public Set<PrivateControlPlaneRegistrationResponse> getItems() {
        return items;
    }

    public void setItems(Set<PrivateControlPlaneRegistrationResponse> items) {
        this.items = items;
    }

    @Override
    public String toString() {
        return "PrivateControlPlaneRegistrationResponses{" +
                "items=" + items +
                '}';
    }
}
