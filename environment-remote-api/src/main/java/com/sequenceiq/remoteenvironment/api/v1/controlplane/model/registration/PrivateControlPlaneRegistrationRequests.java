package com.sequenceiq.remoteenvironment.api.v1.controlplane.model.registration;

import java.util.HashSet;
import java.util.Set;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "PrivateControlPlaneRegistrationV1Requests")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PrivateControlPlaneRegistrationRequests {

    @Schema
    @NotNull
    private Set<PrivateControlPlaneRegistrationRequest> items = new HashSet<>();

    public Set<PrivateControlPlaneRegistrationRequest> getItems() {
        return items;
    }

    public void setItems(Set<PrivateControlPlaneRegistrationRequest> items) {
        this.items = items;
    }

    @Override
    public String toString() {
        return "PrivateControlPlaneRegistrationRequests{" +
                "items=" + items +
                '}';
    }
}
