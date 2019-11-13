package com.sequenceiq.it.cloudbreak.cloud.v4.azure;

import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "integrationtest.azure")
public class AzureProperties {

    private String availabilityZone;

    private String defaultBlueprintName;

    private String region;

    private String location;

    private String blueprintCdhVersion;

    private final Credential credential = new Credential();

    private final Instance instance = new Instance();

    private final Cloudstorage cloudstorage = new Cloudstorage();

    private final Network network = new Network();

    public String getAvailabilityZone() {
        return availabilityZone;
    }

    public void setAvailabilityZone(String availabilityZone) {
        this.availabilityZone = availabilityZone;
    }

    public String getDefaultBlueprintName() {
        return defaultBlueprintName;
    }

    public void setDefaultBlueprintName(String defaultBlueprintName) {
        this.defaultBlueprintName = defaultBlueprintName;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Credential getCredential() {
        return credential;
    }

    public Instance getInstance() {
        return instance;
    }

    public Cloudstorage getCloudstorage() {
        return cloudstorage;
    }

    public Network getNetwork() {
        return network;
    }

    public String getBlueprintCdhVersion() {
        return blueprintCdhVersion;
    }

    public void setBlueprintCdhVersion(String blueprintCdhVersion) {
        this.blueprintCdhVersion = blueprintCdhVersion;
    }

    public static class Credential {
        private String appId;

        private String appPassword;

        private String subscriptionId;

        private String tenantId;

        public String getAppId() {
            return appId;
        }

        public void setAppId(String appId) {
            this.appId = appId;
        }

        public String getAppPassword() {
            return appPassword;
        }

        public void setAppPassword(String appPassword) {
            this.appPassword = appPassword;
        }

        public String getSubscriptionId() {
            return subscriptionId;
        }

        public void setSubscriptionId(String subscriptionId) {
            this.subscriptionId = subscriptionId;
        }

        public String getTenantId() {
            return tenantId;
        }

        public void setTenantId(String tenantId) {
            this.tenantId = tenantId;
        }
    }

    public static class Instance {
        private String type;

        private Integer volumeSize;

        private Integer volumeCount;

        private String volumeType;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Integer getVolumeSize() {
            return volumeSize;
        }

        public void setVolumeSize(Integer volumeSize) {
            this.volumeSize = volumeSize;
        }

        public Integer getVolumeCount() {
            return volumeCount;
        }

        public void setVolumeCount(Integer volumeCount) {
            this.volumeCount = volumeCount;
        }

        public String getVolumeType() {
            return volumeType;
        }

        public void setVolumeType(String volumeType) {
            this.volumeType = volumeType;
        }
    }

    public static class Cloudstorage {

        private final AdlsGen2 adlsGen2 = new AdlsGen2();

        private String accountKey;

        private String accountName;

        private String baseLocation;

        private String fileSystemType;

        private Boolean secure;

        public String getAccountKey() {
            return accountKey;
        }

        public void setAccountKey(String accountKey) {
            this.accountKey = accountKey;
        }

        public String getAccountName() {
            return accountName;
        }

        public void setAccountName(String accountName) {
            this.accountName = accountName;
        }

        public String getBaseLocation() {
            return baseLocation;
        }

        public void setBaseLocation(String baseLocation) {
            this.baseLocation = baseLocation;
        }

        public String getFileSystemType() {
            return fileSystemType;
        }

        public void setFileSystemType(String fileSystemType) {
            this.fileSystemType = fileSystemType;
        }

        public AdlsGen2 getAdlsGen2() {
            return adlsGen2;
        }

        public Boolean getSecure() {
            return secure;
        }

        public void setSecure(Boolean secure) {
            this.secure = secure;
        }

        public static class AdlsGen2 {

            private String assumerIdentity;

            private String loggerIdentity;

            public String getAssumerIdentity() {
                return assumerIdentity;
            }

            public void setAssumerIdentity(String assumerIdentity) {
                this.assumerIdentity = assumerIdentity;
            }

            public String getLoggerIdentity() {
                return loggerIdentity;
            }

            public void setLoggerIdentity(String loggerIdentity) {
                this.loggerIdentity = loggerIdentity;
            }
        }
    }

    public static class Network {
        private String networkId;

        private String resourceGroupName;

        private Boolean noFirewallRules;

        private Boolean noPublicIp;

        private Set<String> subnetIds;

        public Set<String> getSubnetIds() {
            return subnetIds;
        }

        public void setSubnetIds(Set<String> subnetIds) {
            this.subnetIds = subnetIds;
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

        public Boolean getNoFirewallRules() {
            return noFirewallRules;
        }

        public void setNoFirewallRules(Boolean noFirewallRules) {
            this.noFirewallRules = noFirewallRules;
        }

        public Boolean getNoPublicIp() {
            return noPublicIp;
        }

        public void setNoPublicIp(Boolean noPublicIp) {
            this.noPublicIp = noPublicIp;
        }
    }
}
