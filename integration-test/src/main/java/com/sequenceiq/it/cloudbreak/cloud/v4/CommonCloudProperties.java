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

    private String clusterShape;

    private String imageCatalogName;

    private Integer gatewayPort;

    private String defaultCredentialDescription;

    private final Ambari ambari = new Ambari();

    private final ClouderaManager cm = new ClouderaManager();

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

    public String getClusterShape() {
        return clusterShape;
    }

    public void setClusterShape(String clusterShape) {
        this.clusterShape = clusterShape;
    }

    public String getImageCatalogName() {
        return imageCatalogName;
    }

    public void setImageCatalogName(String imageCatalogName) {
        this.imageCatalogName = imageCatalogName;
    }

    public Integer getGatewayPort() {
        return gatewayPort;
    }

    public void setGatewayPort(Integer gatewayPort) {
        this.gatewayPort = gatewayPort;
    }

    public String getDefaultCredentialDescription() {
        return defaultCredentialDescription;
    }

    public void setDefaultCredentialDescription(String defaultCredentialDescription) {
        this.defaultCredentialDescription = defaultCredentialDescription;
    }

    public Ambari getAmbari() {
        return ambari;
    }

    public ClouderaManager getClouderaManager() {
        return cm;
    }

    public static class Ambari {
        private String defaultUser;

        private String defaultPassword;

        private String defaultPort;

        public String getDefaultUser() {
            return defaultUser;
        }

        public void setDefaultUser(String defaultUser) {
            this.defaultUser = defaultUser;
        }

        public String getDefaultPassword() {
            return defaultPassword;
        }

        public void setDefaultPassword(String defaultPassword) {
            this.defaultPassword = defaultPassword;
        }

        public String getDefaultPort() {
            return defaultPort;
        }

        public void setDefaultPort(String defaultPort) {
            this.defaultPort = defaultPort;
        }
    }

    public static class ClouderaManager {
        private String defaultUser;

        private String defaultPassword;

        private String defaultPort;

        public String getDefaultUser() {
            return defaultUser;
        }

        public void setDefaultUser(String defaultUser) {
            this.defaultUser = defaultUser;
        }

        public String getDefaultPassword() {
            return defaultPassword;
        }

        public void setDefaultPassword(String defaultPassword) {
            this.defaultPassword = defaultPassword;
        }

        public String getDefaultPort() {
            return defaultPort;
        }

        public void setDefaultPort(String defaultPort) {
            this.defaultPort = defaultPort;
        }
    }
}
