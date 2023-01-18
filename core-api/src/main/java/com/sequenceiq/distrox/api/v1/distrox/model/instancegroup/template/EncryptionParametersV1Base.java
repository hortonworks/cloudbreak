package com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.TemplateModelDescription;
import com.sequenceiq.common.api.type.EncryptionType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
public class EncryptionParametersV1Base implements Serializable {

    @Schema(description = TemplateModelDescription.ENCRYPTION_TYPE)
    private EncryptionType type;

    @Schema(description = TemplateModelDescription.ENCRYPTION_KEY)
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
