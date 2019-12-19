package com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.annotations.ApiModel;

@ApiModel
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClusterProxyConfiguration {

    private boolean enabled;

    private String url;

    public static ClusterProxyConfiguration disabled() {
        return new ClusterProxyConfiguration();
    }

    public static ClusterProxyConfiguration enabled(String url) {
        ClusterProxyConfiguration clusterProxyConfiguration = new ClusterProxyConfiguration();
        clusterProxyConfiguration.enabled = true;
        clusterProxyConfiguration.setUrl(url);
        return clusterProxyConfiguration;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
