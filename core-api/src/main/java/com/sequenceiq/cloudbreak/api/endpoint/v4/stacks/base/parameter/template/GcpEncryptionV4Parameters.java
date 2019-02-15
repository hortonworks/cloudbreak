package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.KeyEncryptionMethod;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.EncryptionParametersV4Base;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.TemplateModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class GcpEncryptionV4Parameters extends EncryptionParametersV4Base {

    @ApiModelProperty(value = TemplateModelDescription.ENCRYPTION_METHOD, allowableValues = "RAW,RSA,KMS")
    private KeyEncryptionMethod keyEncryptionMethod;

    public KeyEncryptionMethod getKeyEncryptionMethod() {
        return keyEncryptionMethod;
    }

    public void setKeyEncryptionMethod(KeyEncryptionMethod keyEncryptionMethod) {
        this.keyEncryptionMethod = keyEncryptionMethod;
    }
}
