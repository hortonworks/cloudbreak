package com.sequenceiq.consumption.api.v1.consumption.model.response;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.consumption.api.doc.ConsumptionModelDescription;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConsumptionExistenceResponse implements Serializable {

    @NotNull
    @Schema(description = ConsumptionModelDescription.EXISTS, required = true)
    private boolean exists;

    public boolean isExists() {
        return exists;
    }

    public void setExists(boolean exists) {
        this.exists = exists;
    }

    @Override
    public String toString() {
        return "ConsumptionExistenceResponse{" +
                "exists=" + exists +
                '}';
    }
}
