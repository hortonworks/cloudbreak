package com.sequenceiq.it.cloudbreak.newway.cloud.v2;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "integrationtest")
public class CommonCloudProperties {

    private String cloudProvider;

    private String sshPublicKey;

    private String subnetCidr;

    private String imageCatalogName;

    private Integer gatewayPort;

    private String defaultCredentialDescription;

    private final Ambari ambari = new Ambari();

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
}
