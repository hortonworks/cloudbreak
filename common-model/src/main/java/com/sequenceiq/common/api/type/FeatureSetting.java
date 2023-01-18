package com.sequenceiq.common.api.type;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
public class FeatureSetting implements Serializable {

    @NotNull
    @Schema(description = "enabled", required = true)
    private Boolean enabled;

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String toString() {
        return "FeatureSetting{" +
                "enabled=" + enabled +
                '}';
    }
}
