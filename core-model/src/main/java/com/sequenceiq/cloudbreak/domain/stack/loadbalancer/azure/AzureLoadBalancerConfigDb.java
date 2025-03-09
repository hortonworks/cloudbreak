package com.sequenceiq.cloudbreak.domain.stack.loadbalancer.azure;

import com.sequenceiq.common.api.type.LoadBalancerSku;

/**
 * The top level load balancer metadata database object. For Azure, the Azure
 * load balancer name is recorded.
 */
public class AzureLoadBalancerConfigDb {

    private String name;

    private LoadBalancerSku sku;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LoadBalancerSku getSku() {
        return sku;
    }

    public void setSku(LoadBalancerSku sku) {
        this.sku = sku;
    }

    @Override
    public String toString() {
        return "AzureLoadBalancerConfigDb{" +
                "name='" + name + '\'' +
                ", sku=" + sku +
                '}';
    }
}