package com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.requests;

import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.EnvironmentRequestModelDescription;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.CredentialV4Base;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.azure.AzureCredentialV4RequestParameters;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.validation.ValidCredentialV4Request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = EnvironmentRequestModelDescription.CREDENTIAL_DESCRIPTION, parent = CredentialV4Base.class)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
@ValidCredentialV4Request
public class CredentialV4Request extends CredentialV4Base {

    @Valid
    @ApiModelProperty(ModelDescriptions.CredentialModelDescription.AZURE_PARAMETERS)
    private AzureCredentialV4RequestParameters azure;

    public AzureCredentialV4RequestParameters getAzure() {
        return azure;
    }

    public void setAzure(AzureCredentialV4RequestParameters azure) {
        this.azure = azure;
    }

}
