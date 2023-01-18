package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.KeyEncryptionMethod;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.EncryptionParametersV4Base;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.TemplateModelDescription;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
public class GcpEncryptionV4Parameters extends EncryptionParametersV4Base {

    @Schema(description = TemplateModelDescription.ENCRYPTION_METHOD)
    private KeyEncryptionMethod keyEncryptionMethod;

    public KeyEncryptionMethod getKeyEncryptionMethod() {
        return keyEncryptionMethod;
    }

    public void setKeyEncryptionMethod(KeyEncryptionMethod keyEncryptionMethod) {
        this.keyEncryptionMethod = keyEncryptionMethod;
    }
}
