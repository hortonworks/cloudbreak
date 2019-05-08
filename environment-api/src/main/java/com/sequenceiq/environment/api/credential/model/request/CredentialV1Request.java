package com.sequenceiq.environment.api.credential.model.request;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.environment.api.credential.doc.CredentialDescriptor;
import com.sequenceiq.environment.api.credential.doc.CredentialModelDescription;
import com.sequenceiq.environment.api.credential.model.CredentialV1Base;
import com.sequenceiq.environment.api.credential.model.parameters.azure.AzureCredentialV1RequestParameters;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = CredentialDescriptor.CREDENTIAL_NOTES, parent = CredentialV1Base.class)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class CredentialV1Request extends CredentialV1Base {

    @Valid
    @ApiModelProperty(CredentialModelDescription.AZURE_PARAMETERS)
    private AzureCredentialV1RequestParameters azure;

    public AzureCredentialV1RequestParameters getAzure() {
        return azure;
    }

    public void setAzure(AzureCredentialV1RequestParameters azure) {
        this.azure = azure;
    }

}
