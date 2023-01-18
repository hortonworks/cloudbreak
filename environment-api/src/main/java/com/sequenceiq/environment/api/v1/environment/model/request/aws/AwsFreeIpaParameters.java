package com.sequenceiq.environment.api.v1.environment.model.request.aws;

import java.io.Serializable;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AttachedFreeIpaRequestAwsParameters")
@JsonIgnoreProperties(ignoreUnknown = true)
public class AwsFreeIpaParameters implements Serializable {

    @Valid
    @Schema(description = EnvironmentModelDescription.FREEIPA_AWS_SPOT_PARAMETERS)
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
