package com.sequenceiq.sdx.api.model;

import com.sequenceiq.common.api.type.LoadBalancerSku;

public class SdxAzureBase {

    private LoadBalancerSku loadBalancerSku;

    public LoadBalancerSku getLoadBalancerSku() {
        return loadBalancerSku;
    }

    public void setLoadBalancerSku(LoadBalancerSku loadBalancerSku) {
        this.loadBalancerSku = loadBalancerSku;
    }
}
