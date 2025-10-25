package com.sequenceiq.it.cloudbreak.cloud.v4.aws;

import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.it.cloudbreak.cloud.v4.FreeIpaProperties;
import com.sequenceiq.it.cloudbreak.cloud.v4.VerticalScaleProperties;

@Configuration
@ConfigurationProperties(prefix = "integrationtest.aws")
public class AwsProperties {

    private String availabilityZone;

    private String region;

    private String location;

    private String vpcId;

    private Set<String> subnetIds;

    private String publicKeyId;

    private String dynamoTableName;

    private Boolean multiaz;

    private Boolean govCloud;

    private Boolean externalDatabaseSslEnforcementSupported;

    private String embeddedDbUpgradeSourceVersion;

    private final Instance instance = new Instance();

    private final Instance storageOptimizedInstance = new Instance();

    private final Instance arm64Instance = new Instance();

    private final Credential credential = new Credential();

    private final Cloudstorage cloudstorage = new Cloudstorage();

    private final DiskEncryption diskEncryption = new DiskEncryption();

    private FreeIpaProperties freeipa = new FreeIpaProperties();

    private VerticalScaleProperties verticalScale = new VerticalScaleProperties();

    private TrustProperties trustProperties = new TrustProperties();

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

    public Boolean getMultiaz() {
        return multiaz;
    }

    public void setMultiaz(Boolean multiaz) {
        this.multiaz = multiaz;
    }

    public Boolean getGovCloud() {
        return govCloud;
    }

    public void setGovCloud(Boolean govCloud) {
        this.govCloud = govCloud;
    }

    public Boolean getExternalDatabaseSslEnforcementSupported() {
        return Boolean.TRUE.equals(externalDatabaseSslEnforcementSupported);
    }

    public void setExternalDatabaseSslEnforcementSupported(Boolean externalDatabaseSslEnforcementSupported) {
        this.externalDatabaseSslEnforcementSupported = externalDatabaseSslEnforcementSupported;
    }

    public String getEmbeddedDbUpgradeSourceVersion() {
        return embeddedDbUpgradeSourceVersion;
    }

    public void setEmbeddedDbUpgradeSourceVersion(String embeddedDbUpgradeSourceVersion) {
        this.embeddedDbUpgradeSourceVersion = embeddedDbUpgradeSourceVersion;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getVpcId() {
        return vpcId;
    }

    public void setVpcId(String vpcId) {
        this.vpcId = vpcId;
    }

    public Set<String> getSubnetIds() {
        return subnetIds;
    }

    public void setSubnetIds(Set<String> subnetIds) {
        this.subnetIds = subnetIds;
    }

    public String getPublicKeyId() {
        return publicKeyId;
    }

    public void setPublicKeyId(String publicKeyId) {
        this.publicKeyId = publicKeyId;
    }

    public Instance getInstance() {
        return instance;
    }

    public Instance getStorageOptimizedInstance() {
        return storageOptimizedInstance;
    }

    public Instance getArm64Instance() {
        return arm64Instance;
    }

    public Credential getCredential() {
        return credential;
    }

    public Cloudstorage getCloudStorage() {
        return cloudstorage;
    }

    public DiskEncryption getDiskEncryption() {
        return diskEncryption;
    }

    public String getDynamoTableName() {
        return dynamoTableName;
    }

    public void setDynamoTableName(String dynamoTableName) {
        this.dynamoTableName = dynamoTableName;
    }

    public VerticalScaleProperties getVerticalScale() {
        return verticalScale;
    }

    public void setVerticalScale(VerticalScaleProperties verticalScale) {
        this.verticalScale = verticalScale;
    }

    public TrustProperties getTrust() {
        return trustProperties;
    }

    public void setTrust(TrustProperties trustProperties) {
        this.trustProperties = trustProperties;
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

    public static class Credential {
        private String type;

        private String roleArn;

        private String roleArnExtended;

        private String accessKeyId;

        private String secretKey;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getRoleArn() {
            return roleArn;
        }

        public void setRoleArn(String roleArn) {
            this.roleArn = roleArn;
        }

        public String getRoleArnExtended() {
            return roleArnExtended;
        }

        public void setRoleArnExtended(String roleArnExtended) {
            this.roleArnExtended = roleArnExtended;
        }

        public String getAccessKeyId() {
            return accessKeyId;
        }

        public void setAccessKeyId(String accessKeyId) {
            this.accessKeyId = accessKeyId;
        }

        public String getSecretKey() {
            return secretKey;
        }

        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }
    }

    public static class Cloudstorage {

        private final S3 s3 = new S3();

        private String baseLocation;

        private String fileSystemType;

        public S3 getS3() {
            return s3;
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

        public static class S3 {
            private String instanceProfile;

            public String getInstanceProfile() {
                return instanceProfile;
            }

            public void setInstanceProfile(String instanceProfile) {
                this.instanceProfile = instanceProfile;
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

    public static class TrustProperties {
        private Set<String> subnetIds;

        public Set<String> getSubnetIds() {
            return subnetIds;
        }

        public void setSubnetIds(Set<String> subnetIds) {
            this.subnetIds = subnetIds;
        }
    }
}
