package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.EncryptionParametersV4Base;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class AzureEncryptionV4Parameters extends EncryptionParametersV4Base {

    @ApiModelProperty(value = ModelDescriptions.TemplateModelDescription.DISK_ENCRYPTION_SET_ID)
    private String diskEncryptionSetId;

    public String getDiskEncryptionSetId() {
        return diskEncryptionSetId;
    }

    public void setDiskEncryptionSetId(String diskEncryptionSetId) {
        this.diskEncryptionSetId = diskEncryptionSetId;
    }

}
