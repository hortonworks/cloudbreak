package com.sequenceiq.cloudbreak.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.responses.WorkspaceResourceV4Response;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class CredentialResponse extends CredentialBase {

    @ApiModelProperty(ModelDescriptions.ID)
    private Long id;

    @ApiModelProperty(ModelDescriptions.PUBLIC_IN_ACCOUNT)
    private boolean publicInAccount = true;

    @ApiModelProperty(ModelDescriptions.ATTRIBUTES)
    private SecretResponse attributes;

    @ApiModelProperty
    private WorkspaceResourceV4Response workspace;

    @ApiModelProperty(value = ModelDescriptions.GOV_CLOUD_FLAG)
    private Boolean govCloud;

    @JsonProperty("public")
    public boolean isPublicInAccount() {
        return publicInAccount;
    }

    @JsonProperty("id")
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setPublicInAccount(boolean publicInAccount) {
        this.publicInAccount = publicInAccount;
    }

    public SecretResponse getAttributes() {
        return attributes;
    }

    public void setAttributes(SecretResponse attributes) {
        this.attributes = attributes;
    }

    public WorkspaceResourceV4Response getWorkspace() {
        return workspace;
    }

    public void setWorkspace(WorkspaceResourceV4Response workspace) {
        this.workspace = workspace;
    }

    public Boolean getGovCloud() {
        return govCloud;
    }

    public void setGovCloud(Boolean govCloud) {
        this.govCloud = govCloud;
    }
}
