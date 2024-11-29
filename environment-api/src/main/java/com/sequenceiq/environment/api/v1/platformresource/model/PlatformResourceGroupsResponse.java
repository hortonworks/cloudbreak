package com.sequenceiq.environment.api.v1.platformresource.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
@SuppressFBWarnings("SE_BAD_FIELD")
public class PlatformResourceGroupsResponse implements Serializable {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private List<PlatformResourceGroupResponse> resourceGroups = new ArrayList<>();

    public PlatformResourceGroupsResponse() {
    }

    public PlatformResourceGroupsResponse(@NotNull List<PlatformResourceGroupResponse> resourceGroups) {
        this.resourceGroups = resourceGroups;
    }

    public List<PlatformResourceGroupResponse> getResourceGroups() {
        return resourceGroups;
    }

    public void setResourceGroups(List<PlatformResourceGroupResponse> resourceGroups) {
        this.resourceGroups = resourceGroups;
    }

    @Override
    public String toString() {
        return "PlatformResourceGroupsResponse{" +
                "resourceGroups=" + resourceGroups +
                '}';
    }
}
