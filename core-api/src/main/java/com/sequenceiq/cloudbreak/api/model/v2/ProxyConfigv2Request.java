package com.sequenceiq.cloudbreak.api.model.v2;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.model.JsonEntity;
import com.sequenceiq.cloudbreak.api.model.proxy.ProxyConfigRequest;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ClusterModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class ProxyConfigv2Request implements JsonEntity {
    @ApiModelProperty(ClusterModelDescription.PROXY_CONFIG_ID)
    private Long id;

    @Valid
    @ApiModelProperty(ClusterModelDescription.PROXY_CONFIG)
    private ProxyConfigRequest proxyConfigRequest;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ProxyConfigRequest getProxyConfigRequest() {
        return proxyConfigRequest;
    }

    public void setProxyConfigRequest(ProxyConfigRequest proxyConfigRequest) {
        this.proxyConfigRequest = proxyConfigRequest;
    }
}
