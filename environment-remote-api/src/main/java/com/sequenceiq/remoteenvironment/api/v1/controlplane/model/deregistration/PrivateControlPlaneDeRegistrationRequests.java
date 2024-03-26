package com.sequenceiq.remoteenvironment.api.v1.controlplane.model.deregistration;

import java.util.HashSet;
import java.util.Set;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "PrivateControlPlaneDeRegistrationV1Requests")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PrivateControlPlaneDeRegistrationRequests {

    @Schema
    @NotNull
    private Set<PrivateControlPlaneDeRegistrationRequest> items = new HashSet<>();

    public Set<PrivateControlPlaneDeRegistrationRequest> getItems() {
        return items;
    }

    public void setItems(Set<PrivateControlPlaneDeRegistrationRequest> items) {
        this.items = items;
    }

    @Override
    public String toString() {
        return "PrivateControlPlaneDeRegistrationRequests{" +
                "items=" + items +
                '}';
    }
}
