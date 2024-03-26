package com.sequenceiq.remoteenvironment.api.v1.controlplane.model.deregistration;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "PrivateControlPlaneDeRegistrationV1Responses")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PrivateControlPlaneDeRegistrationResponses {

    @Schema
    private Set<PrivateControlPlaneDeRegistrationResponse> items = new HashSet<>();

    public Set<PrivateControlPlaneDeRegistrationResponse> getItems() {
        return items;
    }

    public void setItems(Set<PrivateControlPlaneDeRegistrationResponse> items) {
        this.items = items;
    }

    @Override
    public String toString() {
        return "PrivateControlPlaneDeRegistrationResponses{" +
                "items=" + items +
                '}';
    }
}
