package com.sequenceiq.it.cloudbreak.cloud.v4.gcp;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.it.cloudbreak.cloud.v4.FreeIpaProperties;
import com.sequenceiq.it.cloudbreak.cloud.v4.VerticalScaleProperties;

@Configuration
@ConfigurationProperties(prefix = "integrationtest.gcp")
public class GcpProperties {

    private String availabilityZone;

    private String region;

    private String location;

    private Boolean multiaz;

    private Boolean externalDatabaseSslEnforcementSupported;

    private final Credential credential = new Credential();

    private final Instance instance = new Instance();

    private final SecurityAccess securityAccess = new SecurityAccess();

    private final Baseimage baseimage = new Baseimage();

    private final CloudStorage cloudStorage = new CloudStorage();

    private final Network network = new Network();

    private final DiskEncryption diskEncryption = new DiskEncryption();

    private FreeIpaProperties freeipa = new FreeIpaProperties();

    private VerticalScaleProperties verticalScale = new VerticalScaleProperties();

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

    public Boolean getMultiaz() {
        if (multiaz == null) {
            return Boolean.FALSE;
        }
        return multiaz;
    }

    public void setMultiaz(Boolean multiaz) {
        this.multiaz = multiaz;
    }

    public Boolean getExternalDatabaseSslEnforcementSupported() {
        return Boolean.TRUE.equals(externalDatabaseSslEnforcementSupported);
    }

    public void setExternalDatabaseSslEnforcementSupported(Boolean externalDatabaseSslEnforcementSupported) {
        this.externalDatabaseSslEnforcementSupported = externalDatabaseSslEnforcementSupported;
    }

    public Baseimage getBaseimage() {
        return baseimage;
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

    public DiskEncryption getDiskEncryption() {
        return diskEncryption;
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

        private String sharedProjectId;

        private String networkId;

        private String subnetId;

        private List<String> subnetIds;

        public List<String> getSubnetIds() {
            return subnetIds;
        }

        public void setSubnetIds(List<String> subnetIds) {
            this.subnetIds = subnetIds;
        }

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

        public String getSubnetId() {
            return subnetId;
        }

        public void setSubnetId(String subnetId) {
            this.subnetId = subnetId;
        }

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

        private final Json json = new Json();

        private final P12 p12 = new P12();

        public Json getJson() {
            return json;
        }

        public P12 getP12() {
            return p12;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public static class Json {
            private String base64;

            public String getBase64() {
                return base64;
            }

            public void setBase64(String base64) {
                this.base64 = base64;
            }
        }

        public static class P12 {
            private String base64;

            private String serviceAccountId;

            private String serviceAccountPrivateKey;

            private String projectId;

            public String getBase64() {
                return base64;
            }

            public void setBase64(String base64) {
                this.base64 = base64;
            }

            public String getServiceAccountId() {
                return serviceAccountId;
            }

            public void setServiceAccountId(String serviceAccountId) {
                this.serviceAccountId = serviceAccountId;
            }

            public String getServiceAccountPrivateKey() {
                return serviceAccountPrivateKey;
            }

            public void setServiceAccountPrivateKey(String serviceAccountPrivateKey) {
                this.serviceAccountPrivateKey = serviceAccountPrivateKey;
            }

            public String getProjectId() {
                return projectId;
            }

            public void setProjectId(String projectId) {
                this.projectId = projectId;
            }
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

    public static class CloudStorage {

        private final Gcs gcs = new Gcs();

        private String baseLocation;

        private String fileSystemType;

        private String dataAccessRole;

        private String rangerAuditRole;

        private String rangerAccessAuthorizerRole;

        public Gcs getGcs() {
            return gcs;
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

        public String getDataAccessRole() {
            return dataAccessRole;
        }

        public void setDataAccessRole(String dataAccessRole) {
            this.dataAccessRole = dataAccessRole;
        }

        public String getRangerAuditRole() {
            return rangerAuditRole;
        }

        public void setRangerAuditRole(String rangerAuditRole) {
            this.rangerAuditRole = rangerAuditRole;
        }

        public String getRangerAccessAuthorizerRole() {
            return rangerAccessAuthorizerRole;
        }

        public void setRangerAccessAuthorizerRole(String rangerAccessAuthorizerRole) {
            this.rangerAccessAuthorizerRole = rangerAccessAuthorizerRole;
        }

        public static class Gcs {
            private String serviceAccountEmail;

            public String getServiceAccountEmail() {
                return serviceAccountEmail;
            }

            public void setServiceAccountEmail(String serviceAccountEmail) {
                this.serviceAccountEmail = serviceAccountEmail;
            }
        }
    }

    public static class DiskEncryption {

        private String environmentKey;

        private String datahubKey;

        public String getEnvironmentKey() {
            return environmentKey;
        }

        public void setEnvironmentKey(String environmentKey) {
            this.environmentKey = environmentKey;
        }

        public String getDatahubKey() {
            return datahubKey;
        }

        public void setDatahubKey(String datahubKey) {
            this.datahubKey = datahubKey;
        }
    }
}
