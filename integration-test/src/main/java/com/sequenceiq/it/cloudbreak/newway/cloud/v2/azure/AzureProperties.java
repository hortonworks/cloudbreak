package com.sequenceiq.it.cloudbreak.newway.cloud.v2.azure;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "integrationtest.azure")
public class AzureProperties {

    private String availabilityZone;

    private String defaultBlueprintName;

    private String region;

    private String location;

    private final Credential credential = new Credential();

    private final Instance instance = new Instance();

    private final Cloudstorage cloudstorage = new Cloudstorage();

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
        private String accountKey;

        private String accountName;

        private String locationName;

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

        public String getLocationName() {
            return locationName;
        }

        public void setLocationName(String locationName) {
            this.locationName = locationName;
        }
    }
}
