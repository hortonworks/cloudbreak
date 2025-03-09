package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.loadbalancer;

import java.io.Serializable;

import jakarta.validation.constraints.NotNull;

import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;
import com.sequenceiq.common.api.type.LoadBalancerSku;

import io.swagger.v3.oas.annotations.media.Schema;

public class AzureLoadBalancerResponse implements Serializable {

    @Schema(description = StackModelDescription.AZURE_LB_NAME, requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private String name;

    @Schema(description = StackModelDescription.AZURE_LB_SKU)
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
        return "AzureLoadBalancerResponse{" +
                "name='" + name + '\'' +
                ", sku=" + sku +
                '}';
    }
}