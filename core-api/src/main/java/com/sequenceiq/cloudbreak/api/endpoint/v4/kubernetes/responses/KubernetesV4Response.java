package com.sequenceiq.cloudbreak.api.endpoint.v4.kubernetes.responses;

import com.sequenceiq.cloudbreak.api.endpoint.v4.kubernetes.KubernetesV4Base;
import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.responses.WorkspaceResourceV4Response;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.KubernetesConfig;
import com.sequenceiq.secret.model.SecretResponse;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class KubernetesV4Response extends KubernetesV4Base {

    @ApiModelProperty(ModelDescriptions.ID)
    private Long id;

    @ApiModelProperty(KubernetesConfig.CONFIG)
    private SecretResponse content;

    @ApiModelProperty(ModelDescriptions.WORKSPACE_OF_THE_RESOURCE)
    private WorkspaceResourceV4Response workspace;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SecretResponse getContent() {
        return content;
    }

    public void setContent(SecretResponse content) {
        this.content = content;
    }

    public WorkspaceResourceV4Response getWorkspace() {
        return workspace;
    }

    public void setWorkspace(WorkspaceResourceV4Response workspace) {
        this.workspace = workspace;
    }
}
