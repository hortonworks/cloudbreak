package com.sequenceiq.environment.api.v1.credential.model.request;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.environment.api.doc.ModelDescriptions;
import com.sequenceiq.environment.api.doc.credential.CredentialDescriptor;
import com.sequenceiq.environment.api.doc.credential.CredentialModelDescription;
import com.sequenceiq.environment.api.v1.credential.model.CredentialBase;
import com.sequenceiq.environment.api.v1.credential.model.parameters.azure.AzureCredentialRequestParameters;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = CredentialDescriptor.CREDENTIAL_NOTES, parent = CredentialBase.class, value = "CredentialV1Request")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class EditCredentialRequest extends CredentialBase {

    @ApiModelProperty(value = ModelDescriptions.NAME, required = true)
    private String name;

    @Valid
    @ApiModelProperty(CredentialModelDescription.AZURE_PARAMETERS)
    private AzureCredentialRequestParameters azure;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public AzureCredentialRequestParameters getAzure() {
        return azure;
    }

    public void setAzure(AzureCredentialRequestParameters azure) {
        this.azure = azure;
    }

    @Override
    public String toString() {
        return "CredentialRequest{"
            + "',name='" + getName()
            + "',cloudPlatform='" + getCloudPlatform()
            + "'}";
    }
}
