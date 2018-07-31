package com.sequenceiq.cloudbreak.api.model.v2.template;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.TemplateModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class GcpParameters extends BaseTemplateParameter {

    @ApiModelProperty(TemplateModelDescription.ENCRYPTION)
    private GcpEncryption encryption;

    public GcpEncryption getEncryption() {
        return encryption;
    }

    public void setEncryption(GcpEncryption encryption) {
        this.encryption = encryption;
    }

}
