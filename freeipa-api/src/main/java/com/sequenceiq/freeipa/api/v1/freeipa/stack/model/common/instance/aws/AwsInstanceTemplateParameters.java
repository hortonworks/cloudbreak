package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.aws;

import javax.validation.Valid;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(value = "AwsInstanceTemplateParameters")
public class AwsInstanceTemplateParameters {

    @Valid
    @ApiModelProperty(FreeIpaModelDescriptions.AWS_SPOT_PARAMETERS)
    private AwsInstanceTemplateSpotParameters spot;

    public AwsInstanceTemplateSpotParameters getSpot() {
        return spot;
    }

    public void setSpot(AwsInstanceTemplateSpotParameters spot) {
        this.spot = spot;
    }
}
