package com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.responses;

import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.ATTRIBUTES;
import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.EnvironmentResponseModelDescription;
import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.ID;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.CredentialV4Base;
import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.responses.WorkspaceResourceV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.responses.SecretV4Response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = EnvironmentResponseModelDescription.CREDENTIAL, parent = CredentialV4Base.class)
public class CredentialV4Response extends CredentialV4Base {

    @ApiModelProperty(ID)
    private Long id;

    @ApiModelProperty(ATTRIBUTES)
    private SecretV4Response attributes;

    @ApiModelProperty
    private WorkspaceResourceV4Response workspace;

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
