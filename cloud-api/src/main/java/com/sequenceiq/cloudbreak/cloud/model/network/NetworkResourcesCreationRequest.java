package com.sequenceiq.cloudbreak.cloud.model.network;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Region;

public class NetworkResourcesCreationRequest {

    private final String networkId;

    private final String networkResourceGroup;

    private final boolean existingNetwork;

    private final CloudCredential cloudCredential;

    private final CloudContext cloudContext;

    private final Region region;

    private final String resourceGroup;

    private final boolean privateEndpointsEnabled;

    private final String existingPrivateDnsZoneId;

    private final Map<String, String> tags;

    private NetworkResourcesCreationRequest(Builder builder) {
        networkId = builder.networkId;
        networkResourceGroup = builder.networkResourceGroup;
        existingNetwork = builder.existingNetwork;
        cloudCredential = builder.cloudCredential;
        cloudContext = builder.cloudContext;
        region = builder.region;
        resourceGroup = builder.resourceGroup;
        privateEndpointsEnabled = builder.privateEndpointsEnabled;
        existingPrivateDnsZoneId = builder.existingPrivateDnsZoneId;
        tags = builder.tags;
    }

    public String getNetworkId() {
        return networkId;
    }

    public String getNetworkResourceGroup() {
        return networkResourceGroup;
    }

    public boolean isExistingNetwork() {
        return existingNetwork;
    }

    public CloudCredential getCloudCredential() {
        return cloudCredential;
    }

    public CloudContext getCloudContext() {
        return cloudContext;
    }

    public Region getRegion() {
        return region;
    }

    public String getResourceGroup() {
        return resourceGroup;
    }

    public boolean isPrivateEndpointsEnabled() {
        return privateEndpointsEnabled;
    }

    public String getExistingPrivateDnsZoneId() {
        return existingPrivateDnsZoneId;
    }

    public boolean isExistingPrivateDnsZone() {
        return StringUtils.isNotEmpty(existingPrivateDnsZoneId);
    }

    public Map<String, String> getTags() {
        return tags;
    }

    public static class Builder {

        private String networkId;

        private String networkResourceGroup;

        private boolean existingNetwork;

        private CloudCredential cloudCredential;

        private CloudContext cloudContext;

        private Region region;

        private String resourceGroup;

        private boolean privateEndpointsEnabled;

        private String existingPrivateDnsZoneId;

        private Map<String, String> tags = new HashMap<>();

        public Builder withNetworkId(String networkId) {
            this.networkId = networkId;
            return this;
        }

        public Builder withNetworkResourceGroup(String networkResourceGroup) {
            this.networkResourceGroup = networkResourceGroup;
            return this;
        }

        public Builder withExistingNetwork(boolean existingNetwork) {
            this.existingNetwork = existingNetwork;
            return this;
        }

        public Builder withCloudCredential(CloudCredential cloudCredential) {
            this.cloudCredential = cloudCredential;
            return this;
        }

        public Builder withCloudContext(CloudContext cloudContext) {
            this.cloudContext = cloudContext;
            return this;
        }

        public Builder withRegion(Region region) {
            this.region = region;
            return this;
        }

        public Builder withResourceGroup(String resourceGroup) {
            this.resourceGroup = resourceGroup;
            return this;
        }

        public Builder withPrivateEndpointsEnabled(boolean privateEndpointsEnabled) {
            this.privateEndpointsEnabled = privateEndpointsEnabled;
            return this;
        }

        public Builder withTags(Map<String, String> tags) {
            this.tags = tags;
            return this;
        }

        public Builder withExistingPrivateDnsZone(String existingPrivateDnsZoneId) {
            this.existingPrivateDnsZoneId = existingPrivateDnsZoneId;
            return this;
        }

        public NetworkResourcesCreationRequest build() {
            return new NetworkResourcesCreationRequest(this);
        }
    }
}