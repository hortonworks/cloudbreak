package com.sequenceiq.sdx.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.common.api.type.LoadBalancerSku;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SdxAzureBase {

    @Schema(description = ModelDescriptions.LOAD_BALANCER_SKU)
    private LoadBalancerSku loadBalancerSku;

    public LoadBalancerSku getLoadBalancerSku() {
        return loadBalancerSku;
    }

    public void setLoadBalancerSku(LoadBalancerSku loadBalancerSku) {
        this.loadBalancerSku = loadBalancerSku;
    }
}
