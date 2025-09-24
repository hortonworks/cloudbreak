package com.sequenceiq.cloudbreak.cloud.model;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.common.api.type.OutboundType;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NetworkAttributes implements Serializable {

    private String subnetId;

    private String cloudPlatform;

    private String resourceGroupName;

    private String networkId;

    private OutboundType outboundType;

    private final Class<NetworkAttributes> attributeType = NetworkAttributes.class;

    public NetworkAttributes() {
    }

    @JsonCreator
    public NetworkAttributes(
            @JsonProperty("subnetId") String subnetId,
            @JsonProperty("cloudPlatform") String cloudPlatform,
            @JsonProperty("resourceGroupName") String resourceGroupName,
            @JsonProperty("networkId") String networkId) {
        this.subnetId = subnetId;
        this.cloudPlatform = cloudPlatform;
        this.resourceGroupName = resourceGroupName;
        this.networkId = networkId;
    }

    /**
     * Needed for serialization
     *
     * @return class of the current enum
     */
    public Class<NetworkAttributes> getAttributeType() {
        return attributeType;
    }

    public String getSubnetId() {
        return subnetId;
    }

    public void setSubnetId(String subnetId) {
        this.subnetId = subnetId;
    }

    public String getCloudPlatform() {
        return cloudPlatform;
    }

    public void setCloudPlatform(String cloudPlatform) {
        this.cloudPlatform = cloudPlatform;
    }

    public String getResourceGroupName() {

        return resourceGroupName;
    }

    public void setResourceGroupName(String resourceGroupName) {
        this.resourceGroupName = resourceGroupName;
    }

    public String getNetworkId() {
        return networkId;
    }

    public void setNetworkId(String networkId) {
        this.networkId = networkId;
    }

    public OutboundType getOutboundType() {
        return outboundType == null ? OutboundType.NOT_DEFINED : outboundType;
    }

    public void setOutboundType(OutboundType outboundType) {
        this.outboundType = outboundType;
    }

    @Override
    public String toString() {
        return "NetworkAttributes{" +
                "subnetId='" + subnetId + '\'' +
                ", cloudPlatform='" + cloudPlatform + '\'' +
                ", resourceGroupName='" + resourceGroupName + '\'' +
                ", networkId='" + networkId + '\'' +
                ", outboundType=" + getOutboundType() +
                ", attributeType=" + attributeType +
                '}';
    }
}