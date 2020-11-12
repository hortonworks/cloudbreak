package com.sequenceiq.it.cloudbreak.cloud.v4.gcp;

import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "integrationtest.gcp")
public class GcpProperties {

    private String availabilityZone;

    private String region;

    private String location;

    private String sharedProjectId;

    private String networkId;

    private Set<String> subnetIds;

    private final Credential credential = new Credential();

    private final Instance instance = new Instance();

    private final SecurityAccess securityAccess = new SecurityAccess();

    private final Baseimage baseimage = new Baseimage();

    private final CloudStorage cloudStorage = new CloudStorage();

    private final Network network = new Network();

    public String getSharedProjectId() {
        return sharedProjectId;
    }

    public void setSharedProjectId(String sharedProjectId) {
        this.sharedProjectId = sharedProjectId;
    }

    public String getNetworkId() {
        return networkId;
    }

    public void setNetworkId(String networkId) {
        this.networkId = networkId;
    }

    public Set<String> getSubnetIds() {
        return subnetIds;
    }

    public Baseimage getBaseimage() {
        return baseimage;
    }

    public void setSubnetIds(Set<String> subnets) {
        this.subnetIds = subnets;
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

    public Instance getInstance() {
        return instance;
    }

    public SecurityAccess getSecurityAccess() {
        return securityAccess;
    }

    public CloudStorage getCloudStorage() {
        return cloudStorage;
    }

    public Network getNetwork() {
        return network;
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

    public static class SecurityAccess {

        private String defaultSecurityGroup;

        private String knoxSecurityGroup;

        public String getDefaultSecurityGroup() {
            return defaultSecurityGroup;
        }

        public void setDefaultSecurityGroup(String defaultSecurityGroup) {
            this.defaultSecurityGroup = defaultSecurityGroup;
        }

        public String getKnoxSecurityGroup() {
            return knoxSecurityGroup;
        }

        public void setKnoxSecurityGroup(String knoxSecurityGroup) {
            this.knoxSecurityGroup = knoxSecurityGroup;
        }

    }

    public static class Network {
        private Boolean noPublicIp;

        private Boolean noFirewallRules;

        public Boolean getNoPublicIp() {
            return noPublicIp;
        }

        public void setNoPublicIp(Boolean noPublicIp) {
            this.noPublicIp = noPublicIp;
        }

        public Boolean getNoFirewallRules() {
            return noFirewallRules;
        }

        public void setNoFirewallRules(Boolean noFirewallRules) {
            this.noFirewallRules = noFirewallRules;
        }
    }

    public static class Credential {

        private String type;

        private String json;

        private String p12;

        private String serviceAccountId;

        private String projectId;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getJson() {
            return json;
        }

        public void setJson(String json) {
            this.json = json;
        }

        public String getP12() {
            return p12;
        }

        public void setP12(String p12) {
            this.p12 = p12;
        }

        public String getServiceAccountId() {
            return serviceAccountId;
        }

        public void setServiceAccountId(String serviceAccountId) {
            this.serviceAccountId = serviceAccountId;
        }

        public String getProjectId() {
            return projectId;
        }

        public void setProjectId(String projectId) {
            this.projectId = projectId;
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

    public static class CloudStorage {
        private final Gcs gcs = new Gcs();

        private String baseLocation;

        public Gcs getGcs() {
            return gcs;
        }

        public String getBaseLocation() {
            return baseLocation;
        }

        public void setBaseLocation(String baseLocation) {
            this.baseLocation = baseLocation;
        }

        public static class Gcs {
            private String serviceAccount;

            public String getServiceAccount() {
                return serviceAccount;
            }

            public void setServiceAccount(String serviceAccount) {
                this.serviceAccount = serviceAccount;
            }
        }
    }
}
