package com.sequenceiq.distrox.api.v1.distrox.model;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.common.api.type.LoadBalancerSku;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class AzureDistroXV1Parameters implements Serializable {

    @Schema
    private String resourceGroupName;

    @Deprecated
    @Schema
    private boolean encryptStorage;

    @Schema(description = ModelDescriptions.StackModelDescription.LOAD_BALANCER_SKU)
    private LoadBalancerSku loadBalancerSku = LoadBalancerSku.getDefault();

    public String getResourceGroupName() {
        return resourceGroupName;
    }

    public void setResourceGroupName(String resourceGroupName) {
        this.resourceGroupName = resourceGroupName;
    }

    public boolean isEncryptStorage() {
        return encryptStorage;
    }

    public void setEncryptStorage(boolean encryptStorage) {
        this.encryptStorage = encryptStorage;
    }

    public LoadBalancerSku getLoadBalancerSku() {
        return loadBalancerSku;
    }

    public void setLoadBalancerSku(LoadBalancerSku loadBalancerSku) {
        this.loadBalancerSku = loadBalancerSku;
    }
}
