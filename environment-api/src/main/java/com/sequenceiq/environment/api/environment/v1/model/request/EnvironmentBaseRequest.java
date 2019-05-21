package com.sequenceiq.environment.api.environment.v1.model.request;

import java.util.HashSet;
import java.util.Set;

import com.sequenceiq.environment.api.environment.doc.EnvironmentModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
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
