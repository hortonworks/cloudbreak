package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.TemplateModelDescription;
import com.sequenceiq.common.api.type.EncryptionType;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
public class EncryptionParametersV4Base implements JsonEntity {

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
