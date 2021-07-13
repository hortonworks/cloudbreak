package com.sequenceiq.it.cloudbreak.cloud.v4;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "integrationtest")
public class CommonCloudProperties {

    private String cloudProvider;

    private String sshPublicKey;

    private String subnetCidr;

    private String accessCidr;

    private Map<String, String> tags;

    private String imageCatalogName;

    private String imageCatalogUrl;

    private Integer gatewayPort;

    private ImageValidation imageValidation = new ImageValidation();

    private Ums ums = new Ums();

    private User user = new User();

    public String getCloudProvider() {
        return cloudProvider;
    }

    public void setCloudProvider(String cloudProvider) {
        this.cloudProvider = cloudProvider;
    }

    public String getSshPublicKey() {
        return sshPublicKey;
    }

    public void setSshPublicKey(String sshPublicKey) {
        this.sshPublicKey = sshPublicKey;
    }

    public String getSubnetCidr() {
        return subnetCidr;
    }

    public void setSubnetCidr(String subnetCidr) {
        this.subnetCidr = subnetCidr;
    }

    public String getAccessCidr() {
        return accessCidr;
    }

    public void setAccessCidr(String accessCidr) {
        this.accessCidr = accessCidr;
    }

    public Map<String, String> getTags() {
        return tags;
    }

    public void setTags(Map<String, String> tags) {
        this.tags = tags;
    }

    public String getImageCatalogName() {
        return imageCatalogName;
    }

    public void setImageCatalogName(String imageCatalogName) {
        this.imageCatalogName = imageCatalogName;
    }

    public String getImageCatalogUrl() {
        return imageCatalogUrl;
    }

    public void setImageCatalogUrl(String imageCatalogUrl) {
        this.imageCatalogUrl = imageCatalogUrl;
    }

    public Integer getGatewayPort() {
        return gatewayPort;
    }

    public void setGatewayPort(Integer gatewayPort) {
        this.gatewayPort = gatewayPort;
    }

    public String getDefaultCredentialDescription() {
        return "autotesting credential default description.";
    }

    public ImageValidation getImageValidation() {
        return imageValidation;
    }

    public void setImageValidation(ImageValidation imageValidation) {
        this.imageValidation = imageValidation;
    }

    public Ums getUms() {
        return ums;
    }

    public void setUms(Ums ums) {
        this.ums = ums;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public static class ImageValidation {

        private String sourceCatalogName;

        private String sourceCatalogUrl;

        private String imageUuid;

        private String freeIpaImageCatalog;

        private String freeIpaImageUuid;

        private String os;

        public String getSourceCatalogName() {
            return sourceCatalogName;
        }

        public void setSourceCatalogName(String sourceCatalogName) {
            this.sourceCatalogName = sourceCatalogName;
        }

        public String getSourceCatalogUrl() {
            return sourceCatalogUrl;
        }

        public void setSourceCatalogUrl(String sourceCatalogUrl) {
            this.sourceCatalogUrl = sourceCatalogUrl;
        }

        public String getImageUuid() {
            return imageUuid;
        }

        public void setImageUuid(String imageUuid) {
            this.imageUuid = imageUuid;
        }

        public String getFreeIpaImageCatalog() {
            return freeIpaImageCatalog;
        }

        public void setFreeIpaImageCatalog(String freeIpaImageCatalog) {
            this.freeIpaImageCatalog = freeIpaImageCatalog;
        }

        public String getFreeIpaImageUuid() {
            return freeIpaImageUuid;
        }

        public void setFreeIpaImageUuid(String freeIpaImageUuid) {
            this.freeIpaImageUuid = freeIpaImageUuid;
        }

        public String getOs() {
            return os;
        }

        public void setOs(String os) {
            this.os = os;
        }
    }

    public static class Ums {
        private String accountKey;

        private String deploymentKey;

        public String getAccountKey() {
                return accountKey;
        }

        public void setAccountKey(String accountKey) {
                this.accountKey = accountKey;
        }

        public String getDeploymentKey() {
                return deploymentKey;
        }

        public void setDeploymentKey(String deploymentKey) {
                this.deploymentKey = deploymentKey;
        }
    }

    public static class User {
        private String accesskey;

        private String secretkey;

        private String crn;

        private String name;

        public String getAccessKey() {
            return accesskey;
        }

        public void setAccessKey(String accessKey) {
            this.accesskey = accessKey;
        }

        public String getSecretKey() {
            return secretkey;
        }

        public void setSecretKey(String secretKey) {
            this.secretkey = secretKey;
        }

        public String getCrn() {
            return crn;
        }

        public void setCrn(String crn) {
            this.crn = crn;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
