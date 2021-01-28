package com.sequenceiq.node.health.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DatabusDetails {

    private String endpoint;

    private String proxyUrl;

    private String stream;

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getProxyUrl() {
        return proxyUrl;
    }

    public void setProxyUrl(String proxyUrl) {
        this.proxyUrl = proxyUrl;
    }

    public String getStream() {
        return stream;
    }

    public void setStream(String stream) {
        this.stream = stream;
    }

    @Override
    public String toString() {
        return "DatabusDetails{" +
                "endpoint='" + endpoint + '\'' +
                ", proxyUrl='" + proxyUrl + '\'' +
                ", stream='" + stream + '\'' +
                '}';
    }
}
