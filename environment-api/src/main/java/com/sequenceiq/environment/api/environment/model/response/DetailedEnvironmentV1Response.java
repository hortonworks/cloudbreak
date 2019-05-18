package com.sequenceiq.environment.api.environment.model.response;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.environment.api.environment.doc.EnvironmentModelDescription;
import com.sequenceiq.environment.api.proxy.model.response.ProxyV1Response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class DetailedEnvironmentV1Response extends EnvironmentV1BaseResponse {

    @ApiModelProperty(EnvironmentModelDescription.PROXY_CONFIGS_RESPONSE)
    private Set<ProxyV1Response> proxies = new HashSet<>();

    public void setProxies(Set<ProxyV1Response> proxies) {
        this.proxies = proxies;
    }

    public Set<ProxyV1Response> getProxies() {
        return proxies;
    }
}
