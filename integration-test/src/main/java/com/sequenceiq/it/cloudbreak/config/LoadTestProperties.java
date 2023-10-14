package com.sequenceiq.it.cloudbreak.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "loadtest.config")
public class LoadTestProperties {

    private Integer numThreads;

    private Integer numTenants;

    private Integer numEnvironmentsPerTenant;

    private Integer numDataHubsPerEnvironment;

    private Integer minNodes;

    private Integer maxNodes;

    private String imageCatalogId;

    public Integer getNumThreads() {
        return numThreads;
    }

    public void setNumThreads(Integer numThreads) {
        this.numThreads = numThreads;
    }

    public Integer getNumTenants() {
        return numTenants;
    }

    public void setNumTenants(Integer numTenants) {
        this.numTenants = numTenants;
    }

    public Integer getNumEnvironmentsPerTenant() {
        return numEnvironmentsPerTenant;
    }

    public void setNumEnvironmentsPerTenant(Integer numEnvironmentsPerTenant) {
        this.numEnvironmentsPerTenant = numEnvironmentsPerTenant;
    }

    public Integer getNumDataHubsPerEnvironment() {
        return numDataHubsPerEnvironment;
    }

    public void setNumDataHubsPerEnvironment(Integer numDataHubsPerEnvironment) {
        this.numDataHubsPerEnvironment = numDataHubsPerEnvironment;
    }

    public Integer getMinNodes() {
        return minNodes;
    }

    public void setMinNodes(Integer minNodes) {
        this.minNodes = minNodes;
    }

    public Integer getMaxNodes() {
        return maxNodes;
    }

    public void setMaxNodes(Integer maxNodes) {
        this.maxNodes = maxNodes;
    }

    public String getImageCatalogId() {
        return imageCatalogId;
    }

    public void setImageCatalogId(String imageCatalogId) {
        this.imageCatalogId = imageCatalogId;
    }
}

