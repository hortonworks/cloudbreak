package com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.TemplateModelDescription;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class AzureInstanceTemplateV1Parameters implements Serializable {

    @Schema(description = TemplateModelDescription.AZURE_PRIVATE_ID)
    private String privateId;

    @Schema(description = "by default false")
    private Boolean encrypted = Boolean.FALSE;

    @Schema(description = "by default true")
    private Boolean managedDisk = Boolean.TRUE;

    public Boolean getEncrypted() {
        return encrypted;
    }

    public void setEncrypted(Boolean encrypted) {
        this.encrypted = encrypted;
    }

    public Boolean getManagedDisk() {
        return managedDisk;
    }

    public void setManagedDisk(Boolean managedDisk) {
        this.managedDisk = managedDisk;
    }

    public String getPrivateId() {
        return privateId;
    }

    public void setPrivateId(String privateId) {
        this.privateId = privateId;
    }
}
