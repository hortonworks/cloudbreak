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

    private String sshPrivateKeyPath;

    private Integer sshTimeout;

    private String defaultSshUser;

    private String defaultCredentialDescription;

    private final Recipe recipe = new Recipe();

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

    public String getSshPrivateKeyPath() {
        return sshPrivateKeyPath;
    }

    public void setSshPrivateKeyPath(String sshPrivateKeyPath) {
        this.sshPrivateKeyPath = sshPrivateKeyPath;
    }

    public Integer getSshTimeout() {
        return sshTimeout;
    }

    public void setSshTimeout(Integer sshTimeout) {
        this.sshTimeout = sshTimeout;
    }

    public String getDefaultSshUser() {
        return defaultSshUser;
    }

    public void setDefaultSshUser(String defaultSshUser) {
        this.defaultSshUser = defaultSshUser;
    }

    public Recipe getRecipe() {
        return recipe;
    }

    public Ambari getAmbari() {
        return ambari;
    }

    public static class Recipe {
        private String content;

        private String outputFilePath;

        private String output;

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getOutputFilePath() {
            return outputFilePath;
        }

        public void setOutputFilePath(String outputFilePath) {
            this.outputFilePath = outputFilePath;
        }

        public String getOutput() {
            return output;
        }

        public void setOutput(String output) {
            this.output = output;
        }
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
