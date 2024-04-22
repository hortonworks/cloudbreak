package com.sequenceiq.it.cloudbreak.cloud.v4.azure;

import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.it.cloudbreak.cloud.v4.FreeIpaProperties;
import com.sequenceiq.it.cloudbreak.cloud.v4.MarketplaceProperties;
import com.sequenceiq.it.cloudbreak.cloud.v4.VerticalScaleProperties;

@Configuration
@ConfigurationProperties(prefix = "integrationtest.azure")
public class AzureProperties {

    private String availabilityZone;

    private String region;

    private String location;

    private final Credential credential = new Credential();

    private final Baseimage baseimage = new Baseimage();

    private final Instance instance = new Instance();

    private final Instance storageOptimizedInstance = new Instance();

    private final Cloudstorage cloudstorage = new Cloudstorage();

    private final Network network = new Network();

    private final DiskEncription diskEncryption = new DiskEncription();

    private final Resourcegroup resourcegroup = new Resourcegroup();

    private FreeIpaProperties freeipa = new FreeIpaProperties();

    private VerticalScaleProperties verticalScale = new VerticalScaleProperties();

    private Boolean externalDatabaseSslEnforcementSupported;

    private MarketplaceProperties marketplace = new MarketplaceProperties();

    public VerticalScaleProperties getVerticalScale() {
        return verticalScale;
    }

    public void setVerticalScale(VerticalScaleProperties verticalScale) {
        this.verticalScale = verticalScale;
    }

    public FreeIpaProperties getFreeipa() {
        return freeipa;
    }

    public void setFreeipa(FreeIpaProperties freeipa) {
        this.freeipa = freeipa;
    }

    public String getAvailabilityZone() {
        return availabilityZone;
    }

    public void setAvailabilityZone(String availabilityZone) {
        this.availabilityZone = availabilityZone;
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

    public Baseimage getBaseimage() {
        return baseimage;
    }

    public Instance getInstance() {
        return instance;
    }

    public Instance getStorageOptimizedInstance() {
        return storageOptimizedInstance;
    }

    public Cloudstorage getCloudStorage() {
        return cloudstorage;
    }

    public Network getNetwork() {
        return network;
    }

    public DiskEncription getDiskEncryption() {
        return diskEncryption;
    }

    public Resourcegroup getResourcegroup() {
        return resourcegroup;
    }

    public Boolean getExternalDatabaseSslEnforcementSupported() {
        return Boolean.TRUE.equals(externalDatabaseSslEnforcementSupported);
    }

    public void setExternalDatabaseSslEnforcementSupported(Boolean externalDatabaseSslEnforcementSupported) {
        this.externalDatabaseSslEnforcementSupported = externalDatabaseSslEnforcementSupported;
    }

    public MarketplaceProperties getMarketplace() {
        return marketplace;
    }

    public void setMarketplace(MarketplaceProperties marketplace) {
        this.marketplace = marketplace;
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

    public static class Baseimage {
        private String imageId;

        public String getImageId() {
            return imageId;
        }

        public void setImageId(String imageId) {
            this.imageId = imageId;
        }
    }

    public static class Instance {
        private String type;

        private Integer rootVolumeSize;

        private Integer volumeSize;

        private Integer volumeCount;

        private String volumeType;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Integer getRootVolumeSize() {
            return rootVolumeSize;
        }

        public void setRootVolumeSize(Integer rootVolumeSize) {
            this.rootVolumeSize = rootVolumeSize;
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

        private Integer zeroBlobLengthToleration = 5;

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

        public Integer getZeroBlobLengthToleration() {
            return zeroBlobLengthToleration;
        }

        public void setZeroBlobLengthToleration(Integer zeroBlobLengthToleration) {
            this.zeroBlobLengthToleration = zeroBlobLengthToleration;
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

        private Boolean noPublicIp;

        private Set<String> subnetIds;

        private String databasePrivateDnsZoneId;

        private Set<String> flexibleServerSubnetIds;

        public String getDatabasePrivateDnsZoneId() {
            return databasePrivateDnsZoneId;
        }

        public void setDatabasePrivateDnsZoneId(String databasePrivateDnsZoneId) {
            this.databasePrivateDnsZoneId = databasePrivateDnsZoneId;
        }

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

        public Boolean getNoPublicIp() {
            return noPublicIp;
        }

        public void setNoPublicIp(Boolean noPublicIp) {
            this.noPublicIp = noPublicIp;
        }

        public Set<String> getFlexibleServerSubnetIds() {
            return flexibleServerSubnetIds;
        }

        public void setFlexibleServerSubnetIds(Set<String> flexibleServerSubnetIds) {
            this.flexibleServerSubnetIds = flexibleServerSubnetIds;
        }
    }

    public static class DiskEncription {
        private String encryptionKeyUrl;

        private String resourceGroupName;

        private String managedIdentity;

        public String getEncryptionKeyUrl() {
            return encryptionKeyUrl;
        }

        public void setEncryptionKeyUrl(String encryptionKeyUrl) {
                this.encryptionKeyUrl = encryptionKeyUrl;
            }

        public String getManagedIdentity() {
            return managedIdentity;
        }

        public void setManagedIdentity(String managedIdentity) {
            this.managedIdentity = managedIdentity;
        }

        public String getResourceGroupName() {
                return resourceGroupName;
            }

        public void setResourceGroupName(String resourceGroupName) {
                this.resourceGroupName = resourceGroupName;
            }
    }

    public static class Resourcegroup {
        private String usage;

        private String name;

        public String getUsage() {
            return usage;
        }

        public void setUsage(String usage) {
            this.usage = usage;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
