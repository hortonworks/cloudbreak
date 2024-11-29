package com.sequenceiq.freeipa.api.v1.dns.model;

import java.util.HashSet;
import java.util.Set;

import jakarta.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AddDnsZoneNetworkV1")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AddDnsZoneNetwork {

    @NotEmpty
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String networkId;

    @NotEmpty(message = "subnet must be present and have at least one member")
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Set<String> subnetIds = new HashSet<>();

    public String getNetworkId() {
        return networkId;
    }

    public void setNetworkId(String networkId) {
        this.networkId = networkId;
    }

    public Set<String> getSubnetIds() {
        return subnetIds;
    }

    public void setSubnetIds(Set<String> subnetIds) {
        this.subnetIds = subnetIds;
    }

    @Override
    public String toString() {
        return "AddDnsZoneNetwork{"
                + "networkId='" + networkId + '\''
                + ", subnetIds=" + subnetIds
                + '}';
    }
}
