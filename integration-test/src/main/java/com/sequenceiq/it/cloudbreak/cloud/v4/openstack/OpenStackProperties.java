package com.sequenceiq.it.cloudbreak.cloud.v4.openstack;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "integrationtest.openstack")
public class OpenStackProperties {

    private String defaultBlueprintName;

    private String availabilityZone;

    private String region;

    private String location;

    private String publicNetId;

    private String networkingOption;

    private final Baseimage baseimage = new Baseimage();

    private final Instance instance = new Instance();

    private final Credential credential = new Credential();

    private final Prewarmed prewarmed = new Prewarmed();

    private String blueprintCdhVersion;

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

    public String getPublicNetId() {
        return publicNetId;
    }

    public void setPublicNetId(String publicNetId) {
        this.publicNetId = publicNetId;
    }

    public String getNetworkingOption() {
        return networkingOption;
    }

    public void setNetworkingOption(String networkingOption) {
        this.networkingOption = networkingOption;
    }

    public Baseimage getBaseimage() {
        return baseimage;
    }

    public Instance getInstance() {
        return instance;
    }

    public Credential getCredential() {
        return credential;
    }

    public Prewarmed getPrewarmed() {
        return prewarmed;
    }

    public String getBlueprintCdhVersion() {
        return blueprintCdhVersion;
    }

    public void setBlueprintCdhVersion(String blueprintCdhVersion) {
        this.blueprintCdhVersion = blueprintCdhVersion;
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

    public static class Credential {
        private String endpoint;

        private String tenant;

        private String userName;

        private String password;

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getTenant() {
            return tenant;
        }

        public void setTenant(String tenant) {
            this.tenant = tenant;
        }

        public String getUserName() {
            return userName;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    public static class Prewarmed {

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
}
