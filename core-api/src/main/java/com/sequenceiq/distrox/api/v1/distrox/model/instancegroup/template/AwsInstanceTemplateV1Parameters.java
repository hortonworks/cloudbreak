package com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template;

import java.io.Serializable;

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

    @ApiModelProperty(TemplateModelDescription.AWS_SPOT_PRICE)
    private Double spotPrice;

    @ApiModelProperty(TemplateModelDescription.ENCRYPTION)
    private AwsEncryptionV1Parameters encryption;

    public AwsEncryptionV1Parameters getEncryption() {
        return encryption;
    }

    public void setEncryption(AwsEncryptionV1Parameters encryption) {
        this.encryption = encryption;
    }

    public Double getSpotPrice() {
        return spotPrice;
    }

    public void setSpotPrice(Double spotPrice) {
        this.spotPrice = spotPrice;
    }
}
