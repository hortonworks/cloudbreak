package com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template;

import java.io.Serializable;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.TemplateModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class AwsInstanceTemplateV1Parameters implements Serializable {

    @Valid
    @ApiModelProperty(TemplateModelDescription.AWS_SPOT_PARAMETERS)
    private AwsInstanceTemplateV1SpotParameters spot;

    @ApiModelProperty(TemplateModelDescription.ENCRYPTION)
    private AwsEncryptionV1Parameters encryption;

    public AwsInstanceTemplateV1SpotParameters getSpot() {
        return spot;
    }

    public void setSpot(AwsInstanceTemplateV1SpotParameters spot) {
        this.spot = spot;
    }

    public AwsEncryptionV1Parameters getEncryption() {
        return encryption;
    }

    public void setEncryption(AwsEncryptionV1Parameters encryption) {
        this.encryption = encryption;
    }
}
