package com.sequenceiq.environment.network.dto;

import java.util.Set;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

@JsonDeserialize(builder = AzureParams.Builder.class)
public class AzureParams {

    private String networkId;

    private String resourceGroupName;

    private boolean noPublicIp;

    private String databasePrivateDnsZoneId;

    private String aksPrivateDnsZoneId;

    private boolean noOutboundLoadBalancer;

    private Set<String> availabilityZones;

    private AzureParams(Builder builder) {
        networkId = builder.networkId;
        resourceGroupName = builder.resourceGroupName;
        noPublicIp = builder.noPublicIp;
        databasePrivateDnsZoneId = builder.databasePrivateDnsZoneId;
        noOutboundLoadBalancer = builder.noOutboundLoadBalancer;
        aksPrivateDnsZoneId = builder.aksPrivateDnsZoneId;
        availabilityZones = builder.availabilityZones;
    }

    public String getNetworkId() {
        return networkId;
    }

    public void setNetworkId(String networkId) {
        this.networkId = networkId;
    }

    public String getResourceGroupName() {
        return resourceGroupName;
    }

    public void setResourceGroupName(String resourceGroupName) {
        this.resourceGroupName = resourceGroupName;
    }

    public boolean isNoPublicIp() {
        return noPublicIp;
    }

    public void setNoPublicIp(boolean noPublicIp) {
        this.noPublicIp = noPublicIp;
    }

    public String getDatabasePrivateDnsZoneId() {
        return databasePrivateDnsZoneId;
    }

    public void setDatabasePrivateDnsZoneId(String databasePrivateDnsZoneId) {
        this.databasePrivateDnsZoneId = databasePrivateDnsZoneId;
    }

    public String getAksPrivateDnsZoneId() {
        return aksPrivateDnsZoneId;
    }

    public void setAksPrivateDnsZoneId(String aksPrivateDnsZoneId) {
        this.aksPrivateDnsZoneId = aksPrivateDnsZoneId;
    }

    public boolean isNoOutboundLoadBalancer() {
        return noOutboundLoadBalancer;
    }

    public void setNoOutboundLoadBalancer(boolean noOutboundLoadBalancer) {
        this.noOutboundLoadBalancer = noOutboundLoadBalancer;
    }

    public Set<String> getAvailabilityZones() {
        return availabilityZones;
    }

    public void setAvailabilityZones(Set<String> availabilityZones) {
        this.availabilityZones = availabilityZones;
    }

    @Override
    public String toString() {
        return "AzureParams{" +
                "networkId='" + networkId + '\'' +
                ", resourceGroupName='" + resourceGroupName + '\'' +
                ", noPublicIp=" + noPublicIp +
                ", databasePrivateDnsZoneId='" + databasePrivateDnsZoneId + '\'' +
                ", aksPrivateDnsZoneId='" + aksPrivateDnsZoneId + '\'' +
                ", noOutboundLoadBalancer='" + noOutboundLoadBalancer + '\'' +
                ", availabilityZones='" + availabilityZones + '\'' +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    @JsonPOJOBuilder
    public static final class Builder {
        private String networkId;

        private String resourceGroupName;

        private boolean noPublicIp;

        private String databasePrivateDnsZoneId;

        private String aksPrivateDnsZoneId;

        private boolean noOutboundLoadBalancer;

        private Set<String> availabilityZones;

        private Builder() {
        }

        public Builder withNetworkId(String networkId) {
            this.networkId = networkId;
            return this;
        }

        public Builder withResourceGroupName(String resourceGroupName) {
            this.resourceGroupName = resourceGroupName;
            return this;
        }

        public Builder withNoPublicIp(boolean noPublicIp) {
            this.noPublicIp = noPublicIp;
            return this;
        }

        public Builder withDatabasePrivateDnsZoneId(String databasePrivateDnsZoneId) {
            this.databasePrivateDnsZoneId = databasePrivateDnsZoneId;
            return this;
        }

        public Builder withAksPrivateDnsZoneId(String aksPrivateDnsZoneId) {
            this.aksPrivateDnsZoneId = aksPrivateDnsZoneId;
            return this;
        }

        public Builder withNoOutboundLoadBalancer(boolean noOutboundLoadBalancer) {
            this.noOutboundLoadBalancer = noOutboundLoadBalancer;
            return this;
        }

        public Builder withAvailabilityZones(Set<String> availabilityZones) {
            this.availabilityZones = availabilityZones;
            return this;
        }

        public AzureParams build() {
            return new AzureParams(this);
        }
    }
}
