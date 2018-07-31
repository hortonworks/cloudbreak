package com.sequenceiq.cloudbreak.api.model.v2.template;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.model.JsonEntity;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.TemplateModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class GcpEncryption implements JsonEntity {

    @ApiModelProperty(TemplateModelDescription.GCP_ENCRYPTION_TYPE)
    private String type;

    @ApiModelProperty(TemplateModelDescription.ENCRYPTION_METHOD)
    private String keyEncryptionMethod;

    @ApiModelProperty(TemplateModelDescription.ENCRYPTION_KEY)
    private String key;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getKeyEncryptionMethod() {
        return keyEncryptionMethod;
    }

    public void setKeyEncryptionMethod(String keyEncryptionMethod) {
        this.keyEncryptionMethod = keyEncryptionMethod;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
