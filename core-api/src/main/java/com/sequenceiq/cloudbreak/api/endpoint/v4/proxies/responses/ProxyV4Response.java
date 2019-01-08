package com.sequenceiq.cloudbreak.api.endpoint.v4.proxies.responses;

import com.sequenceiq.cloudbreak.api.model.SecretResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.proxies.ProxyV4Base;
import com.sequenceiq.cloudbreak.api.model.users.WorkspaceResourceResponse;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ClusterModelDescription;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ProxyConfigModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = ModelDescriptions.ProxyConfigModelDescription.DESCRIPTION)
public class ProxyV4Response extends ProxyV4Base {

    @ApiModelProperty(ClusterModelDescription.PROXY_CONFIG_ID)
    private Long id;

    @ApiModelProperty(ProxyConfigModelDescription.USERNAME)
    private SecretResponse userName;

    @ApiModelProperty(ProxyConfigModelDescription.PASSWORD)
    private SecretResponse password;

    @ApiModelProperty(ModelDescriptions.WORKSPACE_OF_THE_RESOURCE)
    private WorkspaceResourceResponse workspace;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SecretResponse getUserName() {
        return userName;
    }

    public void setUserName(SecretResponse userName) {
        this.userName = userName;
    }

    public SecretResponse getPassword() {
        return password;
    }

    public void setPassword(SecretResponse password) {
        this.password = password;
    }

    public WorkspaceResourceResponse getWorkspace() {
        return workspace;
    }

    public void setWorkspace(WorkspaceResourceResponse workspace) {
        this.workspace = workspace;
    }
}
