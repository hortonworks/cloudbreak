package com.sequenceiq.environment.api.credential.model.response;

import static com.sequenceiq.environment.api.credential.doc.CredentialModelDescription.ATTRIBUTES;
import static com.sequenceiq.environment.api.doc.ModelDescriptions.ID;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.environment.api.SecretV4Response;
import com.sequenceiq.environment.api.credential.doc.CredentialDescriptor;
import com.sequenceiq.environment.api.credential.doc.CredentialModelDescription;
import com.sequenceiq.environment.api.credential.model.CredentialV1Base;
import com.sequenceiq.environment.api.credential.model.parameters.azure.AzureCredentialV1ResponseParameters;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = CredentialDescriptor.CREDENTIAL, parent = CredentialV1Base.class)
@JsonInclude(Include.NON_NULL)
public class CredentialV1Response extends CredentialV1Base {

    @ApiModelProperty(ID)
    private Long id;

    @ApiModelProperty(ATTRIBUTES)
    private SecretV4Response attributes;

    @ApiModelProperty(CredentialModelDescription.AZURE_PARAMETERS)
    private AzureCredentialV1ResponseParameters azure;

    public AzureCredentialV1ResponseParameters getAzure() {
        return azure;
    }

    public void setAzure(AzureCredentialV1ResponseParameters azure) {
        this.azure = azure;
    }

    @JsonProperty("id")
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SecretV4Response getAttributes() {
        return attributes;
    }

    public void setAttributes(SecretV4Response attributes) {
        this.attributes = attributes;
    }
}
