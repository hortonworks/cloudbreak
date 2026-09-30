package com.sequenceiq.distrox.api.v1.distrox.model.instancegroup;

import java.io.Serializable;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.PurchaseModel;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class PurshaseModelV1Parameters implements Serializable {

    @NotNull
    @Schema
    private PurchaseModel type = PurchaseModel.ONDEMAND;

    public PurchaseModel getType() {
        return type;
    }

    public void setType(PurchaseModel type) {
        this.type = type;
    }

}
