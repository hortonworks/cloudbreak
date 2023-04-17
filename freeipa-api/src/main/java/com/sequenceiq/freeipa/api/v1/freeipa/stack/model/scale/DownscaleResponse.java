package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.annotations.ApiModel;

@ApiModel("FreeIpaDownscaleV1Response")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DownscaleResponse extends ScaleResponseBase {

    private Set<String> downscaleCandidates;

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