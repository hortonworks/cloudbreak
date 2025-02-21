package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaModelDescriptions.FreeIpaLoadBalancerModelDescriptions;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "FreeIpaLoadBalancerV1Response")
@JsonIgnoreProperties(ignoreUnknown = true)
public class FreeIpaLoadBalancerResponse {

    @Schema(description = FreeIpaLoadBalancerModelDescriptions.RESOURCE_ID, requiredMode = Schema.RequiredMode.REQUIRED)
    private String resourceId;

    @Schema(description = FreeIpaLoadBalancerModelDescriptions.FQDN, requiredMode = Schema.RequiredMode.REQUIRED)
    private String fqdn;

    @Schema(description = FreeIpaLoadBalancerModelDescriptions.PRIVATE_IPS, requiredMode = Schema.RequiredMode.REQUIRED)
    private Set<String> privateIps;

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getFqdn() {
        return fqdn;
    }

    public void setFqdn(String fqdn) {
        this.fqdn = fqdn;
    }

    public Set<String> getPrivateIps() {
        return privateIps;
    }

    public void setPrivateIps(Set<String> privateIps) {
        this.privateIps = privateIps;
    }

    @Override
    public String toString() {
        return "FreeIpaLoadBalancerResponse{" +
                "resourceId='" + resourceId + '\'' +
                ", fqdn='" + fqdn + '\'' +
                ", privateIps=" + privateIps +
                '}';
    }
}
