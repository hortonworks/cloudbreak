package com.sequenceiq.environment.api.v1.environment.model.request.aws;

import java.io.Serializable;

import javax.validation.Valid;

import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(value = "AttachedFreeIpaRequestAwsParameters")
public class AwsFreeIpaParameters implements Serializable {

    @Valid
    @ApiModelProperty(EnvironmentModelDescription.FREEIPA_AWS_SPOT_PARAMETERS)
    private AwsFreeIpaSpotParameters spot;

    public AwsFreeIpaSpotParameters getSpot() {
        return spot;
    }

    public void setSpot(AwsFreeIpaSpotParameters spot) {
        this.spot = spot;
    }

    @Override
    public String toString() {
        return "AwsFreeIpaParameters{" +
                "spot=" + spot +
                '}';
    }
}
