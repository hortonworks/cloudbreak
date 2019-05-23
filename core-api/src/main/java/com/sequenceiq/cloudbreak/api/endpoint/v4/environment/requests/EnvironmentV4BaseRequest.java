package com.sequenceiq.cloudbreak.api.endpoint.v4.environment.requests;

import java.util.HashSet;
import java.util.Set;

import com.sequenceiq.cloudbreak.doc.ModelDescriptions.EnvironmentRequestModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public abstract class EnvironmentV4BaseRequest {

    @ApiModelProperty(EnvironmentRequestModelDescription.KUBERNETES_CONFIGS)
    private Set<String> kubernetes = new HashSet<>();

    public Set<String> getKubernetes() {
        return kubernetes;
    }

    public void setKubernetes(Set<String> kubernetes) {
        this.kubernetes = kubernetes == null ? new HashSet<>() : kubernetes;
    }

}
