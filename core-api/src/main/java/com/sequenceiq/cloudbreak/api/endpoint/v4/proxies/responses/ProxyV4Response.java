package com.sequenceiq.cloudbreak.api.endpoint.v4.proxies.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.responses.SecretV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.proxies.ProxyV4Base;
import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.responses.WorkspaceResourceV4Response;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ClusterModelDescription;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ProxyConfigModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = ProxyConfigModelDescription.DESCRIPTION)
@JsonInclude(Include.NON_NULL)
public class ProxyV4Response extends ProxyV4Base {

    @ApiModelProperty(ClusterModelDescription.PROXY_CONFIG_ID)
    private Long id;

    @ApiModelProperty(ProxyConfigModelDescription.USERNAME)
    private SecretV4Response userName;

    @ApiModelProperty(ProxyConfigModelDescription.PASSWORD)
    private SecretV4Response password;

    @ApiModelProperty(ModelDescriptions.WORKSPACE_OF_THE_RESOURCE)
    private WorkspaceResourceV4Response workspace;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SecretV4Response getUserName() {
        return userName;
    }

    public void setUserName(SecretV4Response userName) {
        this.userName = userName;
    }

    public SecretV4Response getPassword() {
        return password;
    }

    public void setPassword(SecretV4Response password) {
        this.password = password;
    }

    public WorkspaceResourceV4Response getWorkspace() {
        return workspace;
    }

    public void setWorkspace(WorkspaceResourceV4Response workspace) {
        this.workspace = workspace;
    }
}
