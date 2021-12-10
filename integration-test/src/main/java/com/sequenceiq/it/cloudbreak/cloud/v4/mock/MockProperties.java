package com.sequenceiq.it.cloudbreak.cloud.v4.mock;

import java.util.List;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "integrationtest.mock")
public class MockProperties {

    private String availabilityZone;

    private String region;

    private String location;

    private String vpcId;

    private Set<String> subnetIds;

    private String publicKeyId;

    private String internetGateway;

    private final Instance instance = new Instance();

    private final Credential credential = new Credential();

    private final Baseimage baseimage = new Baseimage();

    private final Cloudstorage cloudstorage = new Cloudstorage();

    private final DiskEncryption diskEncryption = new DiskEncryption();

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

    public Credential getCredential() {
        return credential;
    }

    public Baseimage getBaseimage() {
        return baseimage;
    }

    public Cloudstorage getCloudStorage() {
        return cloudstorage;
    }

    public DiskEncryption getDiskEncryption() {
        return diskEncryption;
    }

    public String getInternetGateway() {
        return internetGateway;
    }

    public void setInternetGateway(String internetGateway) {
        this.internetGateway = internetGateway;
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
        private String type;

        private String roleArn;

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

    public static class Baseimage {
        private final Redhat7 redhat7 = new Redhat7();

        private final Amazonlinux2 amazonlinux2 = new Amazonlinux2();

        private final Sles12 sles12 = new Sles12();

        public Redhat7 getRedhat7() {
            return redhat7;
        }

        public Amazonlinux2 getAmazonlinux2() {
            return amazonlinux2;
        }

        public Sles12 getSles12() {
            return sles12;
        }

        public static class Redhat7 {
            private String imageId;

            private List<String> blueprints;

            public String getImageId() {
                return imageId;
            }

            public void setImageId(String imageId) {
                this.imageId = imageId;
            }

            public List<String> getBlueprints() {
                return blueprints;
            }

            public void setBlueprints(List<String> blueprints) {
                this.blueprints = blueprints;
            }
        }

        public static class Amazonlinux2 {
            private String imageId;

            private List<String> blueprints;

            public String getImageId() {
                return imageId;
            }

            public void setImageId(String imageId) {
                this.imageId = imageId;
            }

            public List<String> getBlueprints() {
                return blueprints;
            }

            public void setBlueprints(List<String> blueprints) {
                this.blueprints = blueprints;
            }
        }

        public static class Sles12 {
            private String imageId;

            private List<String> blueprints;

            public String getImageId() {
                return imageId;
            }

            public void setImageId(String imageId) {
                this.imageId = imageId;
            }

            public List<String> getBlueprints() {
                return blueprints;
            }

            public void setBlueprints(List<String> blueprints) {
                this.blueprints = blueprints;
            }
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
}
