package com.sequenceiq.consumption.api.v1.consumption.model.response;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.consumption.api.doc.ConsumptionModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConsumptionExistenceResponse implements Serializable {

    @NotNull
    @ApiModelProperty(value = ConsumptionModelDescription.EXISTS, required = true)
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
