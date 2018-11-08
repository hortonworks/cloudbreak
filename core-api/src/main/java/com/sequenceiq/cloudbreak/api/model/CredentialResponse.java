package com.sequenceiq.cloudbreak.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.api.model.users.WorkspaceResourceResponse;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class CredentialResponse extends CredentialBase {

    @ApiModelProperty(ModelDescriptions.ID)
    private Long id;

    @ApiModelProperty(ModelDescriptions.PUBLIC_IN_ACCOUNT)
    private boolean publicInAccount = true;

    @ApiModelProperty
    private WorkspaceResourceResponse workspace;

    @ApiModelProperty(ModelDescriptions.CredentialModelDescription.PARAMETERS_PATH)
    private String parametersPath;

    @JsonProperty("public")
    public boolean isPublicInAccount() {
        return publicInAccount;
    }

    @JsonProperty("id")
    public Long getId() {
        return id;
    }

    public void setPublicInAccount(boolean publicInAccount) {
        this.publicInAccount = publicInAccount;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public WorkspaceResourceResponse getWorkspace() {
        return workspace;
    }

    public void setWorkspace(WorkspaceResourceResponse workspace) {
        this.workspace = workspace;
    }

    public void setParametersPath(String parametersPath) {
        this.parametersPath = parametersPath;
    }

    public String getParametersPath() {
        return parametersPath;
    }
}
