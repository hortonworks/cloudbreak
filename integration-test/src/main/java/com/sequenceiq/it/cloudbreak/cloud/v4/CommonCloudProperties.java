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

    public static class ImageValidation {

        private String sourceCatalogName;

        private String sourceCatalogUrl;

        private String expectedDefaultImageUuid;

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

        public String getExpectedDefaultImageUuid() {
            return expectedDefaultImageUuid;
        }

        public void setExpectedDefaultImageUuid(String expectedDefaultImageUuid) {
            this.expectedDefaultImageUuid = expectedDefaultImageUuid;
        }
    }
}
