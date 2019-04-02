package com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.responses;

import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.ATTRIBUTES;
import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.EnvironmentResponseModelDescription;
import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.ID;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.responses.SecretV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.CredentialV4Base;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.azure.AzureCredentialV4ResponseParameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.responses.WorkspaceResourceV4Response;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = EnvironmentResponseModelDescription.CREDENTIAL, parent = CredentialV4Base.class)
@JsonInclude(Include.NON_NULL)
public class CredentialV4Response extends CredentialV4Base {

    @ApiModelProperty(ID)
    private Long id;

    @ApiModelProperty(ATTRIBUTES)
    private SecretV4Response attributes;

    @ApiModelProperty
    private WorkspaceResourceV4Response workspace;

    @ApiModelProperty(ModelDescriptions.CredentialModelDescription.AZURE_PARAMETERS)
    private AzureCredentialV4ResponseParameters azure;

    public AzureCredentialV4ResponseParameters getAzure() {
        return azure;
    }

    public void setAzure(AzureCredentialV4ResponseParameters azure) {
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

    public WorkspaceResourceV4Response getWorkspace() {
        return workspace;
    }

    public void setWorkspace(WorkspaceResourceV4Response workspace) {
        this.workspace = workspace;
    }

}
