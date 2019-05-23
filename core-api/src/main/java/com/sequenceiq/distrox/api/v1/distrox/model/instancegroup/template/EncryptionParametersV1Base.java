package com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.common.type.EncryptionType;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.TemplateModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class EncryptionParametersV1Base implements Serializable {

    @ApiModelProperty(value = TemplateModelDescription.ENCRYPTION_TYPE, allowableValues = "DEFAULT,NONE,CUSTOM")
    private EncryptionType type;

    @ApiModelProperty(TemplateModelDescription.ENCRYPTION_KEY)
    private String key;

    public EncryptionType getType() {
        return type;
    }

    public void setType(EncryptionType type) {
        this.type = type;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
