package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "FreeIpaDownscaleV1Response")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DownscaleResponse extends ScaleResponseBase {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Set<String> downscaleCandidates = new HashSet<>();

    public Set<String> getDownscaleCandidates() {
        return downscaleCandidates;
    }

    public void setDownscaleCandidates(Set<String> downscaleCandidates) {
        this.downscaleCandidates = downscaleCandidates;
    }

    @Override
    public String toString() {
        return "DownscaleResponse{" +
                "downscaleCandidates=" + downscaleCandidates +
                "} " + super.toString();
    }
}