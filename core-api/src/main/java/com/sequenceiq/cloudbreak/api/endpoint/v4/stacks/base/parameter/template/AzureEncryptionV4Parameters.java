package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.EncryptionParametersV4Base;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
public class AzureEncryptionV4Parameters extends EncryptionParametersV4Base {

    @Schema(description = ModelDescriptions.TemplateModelDescription.DISK_ENCRYPTION_SET_ID)
    private String diskEncryptionSetId;

    @Schema(description = ModelDescriptions.TemplateModelDescription.ENCRYPTION_AT_HOST_ENABLED)
    private Boolean encryptionAtHostEnabled;

    public String getDiskEncryptionSetId() {
        return diskEncryptionSetId;
    }

    public void setDiskEncryptionSetId(String diskEncryptionSetId) {
        this.diskEncryptionSetId = diskEncryptionSetId;
    }

    public Boolean getEncryptionAtHostEnabled() {
        return encryptionAtHostEnabled;
    }

    public void setEncryptionAtHostEnabled(Boolean encryptionAtHostEnabled) {
        this.encryptionAtHostEnabled = encryptionAtHostEnabled;
    }

}
