package com.sequenceiq.cloudbreak.api.model.proxy;

import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("ProxyConfigResponse")
public class ProxyConfigResponse extends ProxyConfigBase {
    @ApiModelProperty(ModelDescriptions.ClusterModelDescription.PROXY_CONFIG_ID)
    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
