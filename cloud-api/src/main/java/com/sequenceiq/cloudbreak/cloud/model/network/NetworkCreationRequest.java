package com.sequenceiq.cloudbreak.cloud.model.network;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Region;

public class NetworkCreationRequest {

    private final Long envId;

    private final String envName;

    private final String envCrn;

    private final CloudCredential cloudCredential;

    private final String variant;

    private final String userName;

    private final String creatorCrn;

    private final Region region;

    private final String networkCidr;

    private final Set<NetworkSubnetRequest> publicSubnets;

    private final Set<NetworkSubnetRequest> privateSubnets;

    private final String resourceGroup;

    private final boolean noPublicIp;

    private final String stackName;

    private final boolean privateSubnetEnabled;

    private final Map<String, String> tags;

    private NetworkCreationRequest(Builder builder) {
        envId = builder.envId;
        envName = builder.envName;
        envCrn = builder.envCrn;
        cloudCredential = builder.cloudCredential;
        variant = builder.variant;
        region = builder.region;
        networkCidr = builder.networkCidr;
        publicSubnets = builder.publicSubnets;
        privateSubnets = builder.privateSubnets;
        resourceGroup = builder.resourceGroup;
        noPublicIp = builder.noPublicIp;
        stackName = builder.stackName;
        privateSubnetEnabled = builder.privateSubnetEnabled;
        userName = builder.userName;
        creatorCrn = builder.creatorCrn;
        tags = builder.tags;
    }

    public String getEnvName() {
        return envName;
    }

    public String getEnvCrn() {
        return envCrn;
    }

    public CloudCredential getCloudCredential() {
        return cloudCredential;
    }

    public String getVariant() {
        return variant;
    }

    public Region getRegion() {
        return region;
    }

    public String getNetworkCidr() {
        return networkCidr;
    }

    public Set<NetworkSubnetRequest> getPrivateSubnets() {
        return privateSubnets;
    }

    public Set<NetworkSubnetRequest> getPublicSubnets() {
        return publicSubnets;
    }

    public String getResourceGroup() {
        return resourceGroup;
    }

    public boolean isNoPublicIp() {
        return noPublicIp;
    }

    public String getStackName() {
        return stackName;
    }

    public Long getEnvId() {
        return envId;
    }

    public boolean isPrivateSubnetEnabled() {
        return privateSubnetEnabled;
    }

    public String getUserName() {
        return userName;
    }

    public String getCreatorCrn() {
        return creatorCrn;
    }

    public Map<String, String> getTags() {
        return tags;
    }

    public static class Builder {
        private Long envId;

        private String envName;

        private String envCrn;

        private String userName;

        private CloudCredential cloudCredential;

        private String variant;

        private Region region;

        private String networkCidr;

        private Set<NetworkSubnetRequest> publicSubnets;

        private Set<NetworkSubnetRequest> privateSubnets;

        private String resourceGroup;

        private boolean noPublicIp;

        private String stackName;

        private boolean privateSubnetEnabled;

        private String creatorCrn;

        private Map<String, String> tags = new HashMap<>();

        public Builder withEnvId(Long envId) {
            this.envId = envId;
            return this;
        }

        public Builder withEnvName(String envName) {
            this.envName = envName;
            return this;
        }

        public Builder withEnvCrn(String envCrn) {
            this.envCrn = envCrn;
            return this;
        }

        public Builder withCloudCredential(CloudCredential cloudCredential) {
            this.cloudCredential = cloudCredential;
            return this;
        }

        public Builder withVariant(String variant) {
            this.variant = variant;
            return this;
        }

        public Builder withRegion(Region region) {
            this.region = region;
            return this;
        }

        public Builder withNetworkCidr(String networkCidr) {
            this.networkCidr = networkCidr;
            return this;
        }

        public Builder withPublicSubnets(Set<NetworkSubnetRequest> subnets) {
            this.publicSubnets = subnets;
            return this;
        }

        public Builder withPrivateSubnets(Set<NetworkSubnetRequest> subnets) {
            this.privateSubnets = subnets;
            return this;
        }

        public Builder withResourceGroup(String resourceGroup) {
            this.resourceGroup = resourceGroup;
            return this;
        }

        public Builder withNoPublicIp(boolean noPublicIp) {
            this.noPublicIp = noPublicIp;
            return this;
        }

        public Builder withStackName(String stackName) {
            this.stackName = stackName;
            return this;
        }

        public Builder withPrivateSubnetEnabled(boolean privateSubnetEnabled) {
            this.privateSubnetEnabled = privateSubnetEnabled;
            return this;
        }

        public Builder withUserName(String userName) {
            this.userName = userName;
            return this;
        }

        public Builder withCreatorCrn(String creatorCrn) {
            this.creatorCrn = creatorCrn;
            return this;
        }

        public Builder withTags(Map<String, String> tags) {
            this.tags = tags;
            return this;
        }

        public NetworkCreationRequest build() {
            return new NetworkCreationRequest(this);
        }
    }

}
