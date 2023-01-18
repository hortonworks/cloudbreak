package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.aws;

import java.io.Serializable;

import javax.validation.Valid;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaModelDescriptions;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AwsInstanceTemplateParameters")
public class AwsInstanceTemplateParameters implements Serializable {

    @Valid
    @Schema(description = FreeIpaModelDescriptions.AWS_SPOT_PARAMETERS)
    private AwsInstanceTemplateSpotParameters spot;

    public AwsInstanceTemplateSpotParameters getSpot() {
        return spot;
    }

    public void setSpot(AwsInstanceTemplateSpotParameters spot) {
        this.spot = spot;
    }

    @Override
    public String toString() {
        return "AwsInstanceTemplateParameters{" +
                "spot=" + spot +
                '}';
    }
}
