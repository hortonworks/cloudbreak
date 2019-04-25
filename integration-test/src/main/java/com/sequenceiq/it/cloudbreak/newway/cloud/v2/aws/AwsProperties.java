package com.sequenceiq.it.cloudbreak.newway.cloud.v2.aws;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "integrationtest.aws")
public class AwsProperties {

    private String availabilityZone;

    private String region;

    private String location;

    private String vpcId;

    private String subnetId;

    private String publicKeyId;

    private String defaultBlueprintName;

    private final Instance instance = new Instance();

    private final Credential credential = new Credential();

    private final Baseimage baseimage = new Baseimage();

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

    public String getSubnetId() {
        return subnetId;
    }

    public void setSubnetId(String subnetId) {
        this.subnetId = subnetId;
    }

    public String getPublicKeyId() {
        return publicKeyId;
    }

    public void setPublicKeyId(String publicKeyId) {
        this.publicKeyId = publicKeyId;
    }

    public String getDefaultBlueprintName() {
        return defaultBlueprintName;
    }

    public void setDefaultBlueprintName(String defaultBlueprintName) {
        this.defaultBlueprintName = defaultBlueprintName;
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
}
