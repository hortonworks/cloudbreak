package com.sequenceiq.it.cloudbreak.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "integrationtest.preflight.endpoints")
public class CloudProviderEndpointsProperties {

    private List<String> aws = List.of();

    private List<String> azure = List.of();

    private List<String> gcp = List.of();

    public List<String> getAws() {
        return aws;
    }

    public void setAws(List<String> aws) {
        this.aws = aws;
    }

    public List<String> getAzure() {
        return azure;
    }

    public void setAzure(List<String> azure) {
        this.azure = azure;
    }

    public List<String> getGcp() {
        return gcp;
    }

    public void setGcp(List<String> gcp) {
        this.gcp = gcp;
    }
}
