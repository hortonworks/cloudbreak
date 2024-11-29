package com.sequenceiq.environment.api.v1.environment.model;

import java.util.HashSet;
import java.util.Set;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "EnvironmentNetworkAzureV1Params")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class EnvironmentNetworkAzureParams {

    @Size(max = 255)
    @Schema(description = EnvironmentModelDescription.AZURE_NETWORK_ID, requiredMode = Schema.RequiredMode.REQUIRED)
    private String networkId;

    @Size(max = 255)
    @Schema(description = EnvironmentModelDescription.AZURE_RESOURCE_GROUP_NAME, requiredMode = Schema.RequiredMode.REQUIRED)
    private String resourceGroupName;

    @Size(max = 255)
    @Schema(description = EnvironmentModelDescription.AZURE_PRIVATE_DNS_ZONE_ID)
    private String databasePrivateDnsZoneId;

    @Size(max = 255)
    @Schema(description = EnvironmentModelDescription.AZURE_AKS_PRIVATE_DNS_ZONE_ID)
    private String aksPrivateDnsZoneId;

    @NotNull
    @Schema(description = EnvironmentModelDescription.AZURE_NO_PUBLIC_IP, requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean noPublicIp = Boolean.FALSE;

    @Schema(description = EnvironmentModelDescription.NO_OUTBOUND_LOAD_BALANCER)
    private Boolean noOutboundLoadBalancer = Boolean.FALSE;

    @Schema(description = EnvironmentModelDescription.AZURE_AVAILABILITY_ZONES)
    private Set<String> availabilityZones;

    @Schema(description = EnvironmentModelDescription.AZURE_DELEGATED_FLEXIBLE_SERVER_SUBNET_IDS)
    private Set<String> flexibleServerSubnetIds;

    @Schema(description = EnvironmentModelDescription.AZURE_USE_PUBLIC_DNS_FOR_PRIVATE_AKS)
    private Boolean usePublicDnsForPrivateAks = Boolean.FALSE;

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

    public Boolean getNoPublicIp() {
        return noPublicIp;
    }

    public void setNoPublicIp(Boolean noPublicIp) {
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

    public Boolean getNoOutboundLoadBalancer() {
        return noOutboundLoadBalancer;
    }

    public void setNoOutboundLoadBalancer(Boolean noOutboundLoadBalancer) {
        this.noOutboundLoadBalancer = noOutboundLoadBalancer;
    }

    public Set<String> getAvailabilityZones() {
        return availabilityZones;
    }

    public void setAvailabilityZones(Set<String> availabilityZones) {
        this.availabilityZones = availabilityZones;
    }

    public Set<String> getFlexibleServerSubnetIds() {
        return flexibleServerSubnetIds;
    }

    public void setFlexibleServerSubnetIds(Set<String> flexibleServerSubnetIds) {
        this.flexibleServerSubnetIds = flexibleServerSubnetIds;
    }

    public Boolean getUsePublicDnsForPrivateAks() {
        return usePublicDnsForPrivateAks;
    }

    public void setUsePublicDnsForPrivateAks(Boolean usePublicDnsForPrivateAks) {
        this.usePublicDnsForPrivateAks = usePublicDnsForPrivateAks;
    }

    @Override
    public String toString() {
        return "EnvironmentNetworkAzureParams{" +
                "networkId='" + networkId + '\'' +
                ", resourceGroupName='" + resourceGroupName + '\'' +
                ", databasePrivateDnsZoneId='" + databasePrivateDnsZoneId + '\'' +
                ", aksPrivateDnsZoneId='" + aksPrivateDnsZoneId + '\'' +
                ", noPublicIp=" + noPublicIp +
                ", noOutboundLoadBalancer=" + noOutboundLoadBalancer +
                ", availabilityZones=" + availabilityZones +
                ", flexibleServerSubnetIds=" + flexibleServerSubnetIds +
                ", usePublicDnsForPrivateAks=" + usePublicDnsForPrivateAks +
                '}';
    }

    public static final class EnvironmentNetworkAzureParamsBuilder {
        private String networkId;

        private String resourceGroupName;

        private Boolean noPublicIp = Boolean.FALSE;

        private String databasePrivateDnsZoneId;

        private String aksPrivateDnsZoneId;

        private Boolean noOutboundLoadBalancer = Boolean.FALSE;

        private Set<String> availabilityZones = new HashSet<>();

        private Set<String> flexibleServerSubnetIds = new HashSet<>();

        private Boolean usePublicDnsForPrivateAks = Boolean.FALSE;

        private EnvironmentNetworkAzureParamsBuilder() {
        }

        public static EnvironmentNetworkAzureParamsBuilder anEnvironmentNetworkAzureParams() {
            return new EnvironmentNetworkAzureParamsBuilder();
        }

        public EnvironmentNetworkAzureParamsBuilder withNetworkId(String networkId) {
            this.networkId = networkId;
            return this;
        }

        public EnvironmentNetworkAzureParamsBuilder withResourceGroupName(String resourceGroupName) {
            this.resourceGroupName = resourceGroupName;
            return this;
        }

        public EnvironmentNetworkAzureParamsBuilder withNoPublicIp(Boolean noPublicIp) {
            this.noPublicIp = noPublicIp;
            return this;
        }

        public EnvironmentNetworkAzureParamsBuilder withDatabasePrivateDnsZoneId(String databasePrivateDnsZoneId) {
            this.databasePrivateDnsZoneId = databasePrivateDnsZoneId;
            return this;
        }

        public EnvironmentNetworkAzureParamsBuilder withAksPrivateDnsZoneId(String aksPrivateDnsZoneId) {
            this.aksPrivateDnsZoneId = aksPrivateDnsZoneId;
            return this;
        }

        public EnvironmentNetworkAzureParamsBuilder withNoOutboundLoadBalancer(Boolean noOutboundLoadBalancer) {
            this.noOutboundLoadBalancer = noOutboundLoadBalancer;
            return this;
        }

        public EnvironmentNetworkAzureParamsBuilder withAvailabilityZones(Set<String> availabilityZones) {
            this.availabilityZones = availabilityZones;
            return this;
        }

        public EnvironmentNetworkAzureParamsBuilder withFlexibleServerSubnetIds(Set<String> flexibleServerSubnetIds) {
            this.flexibleServerSubnetIds = flexibleServerSubnetIds;
            return this;
        }

        public EnvironmentNetworkAzureParamsBuilder withUsePublicDnsForPrivateAks(Boolean usePublicDnsForPrivateAks) {
            this.usePublicDnsForPrivateAks = usePublicDnsForPrivateAks;
            return this;
        }

        public EnvironmentNetworkAzureParams build() {
            EnvironmentNetworkAzureParams environmentNetworkAzureParams = new EnvironmentNetworkAzureParams();
            environmentNetworkAzureParams.setNetworkId(networkId);
            environmentNetworkAzureParams.setResourceGroupName(resourceGroupName);
            environmentNetworkAzureParams.setNoPublicIp(noPublicIp);
            environmentNetworkAzureParams.setDatabasePrivateDnsZoneId(databasePrivateDnsZoneId);
            environmentNetworkAzureParams.setAksPrivateDnsZoneId(aksPrivateDnsZoneId);
            environmentNetworkAzureParams.setNoOutboundLoadBalancer(noOutboundLoadBalancer);
            environmentNetworkAzureParams.setAvailabilityZones(availabilityZones);
            environmentNetworkAzureParams.setFlexibleServerSubnetIds(flexibleServerSubnetIds);
            environmentNetworkAzureParams.setUsePublicDnsForPrivateAks(usePublicDnsForPrivateAks);
            return environmentNetworkAzureParams;
        }
    }
}
