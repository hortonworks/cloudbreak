package com.sequenceiq.environment.api.v1.environment.model.request;

import java.util.HashSet;
import java.util.Set;

import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(subTypes = {EnvironmentAttachRequest.class, EnvironmentDetachRequest.class, EnvironmentRequest.class})
public abstract class EnvironmentBaseRequest {

    @ApiModelProperty(EnvironmentModelDescription.PROXY_CONFIGS_REQUEST)
    private Set<String> proxies = new HashSet<>();

    public Set<String> getProxies() {
        return proxies;
    }

    public void setProxies(Set<String> proxies) {
        this.proxies = proxies == null ? new HashSet<>() : proxies;
    }
}
