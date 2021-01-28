package com.sequenceiq.node.health.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NodeHealth {

    private String host;

    private String responseStatus;

    private MeteringDetails meteringDetails;

    private NetworkDetails networkDetails;

    private ServicesDetails servicesDetails;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getResponseStatus() {
        return responseStatus;
    }

    public void setResponseStatus(String responseStatus) {
        this.responseStatus = responseStatus;
    }

    public MeteringDetails getMeteringDetails() {
        return meteringDetails;
    }

    public void setMeteringDetails(MeteringDetails meteringDetails) {
        this.meteringDetails = meteringDetails;
    }

    public NetworkDetails getNetworkDetails() {
        return networkDetails;
    }

    public void setNetworkDetails(NetworkDetails networkDetails) {
        this.networkDetails = networkDetails;
    }

    public ServicesDetails getServicesDetails() {
        return servicesDetails;
    }

    public void setServicesDetails(ServicesDetails servicesDetails) {
        this.servicesDetails = servicesDetails;
    }

    @Override
    public String toString() {
        String result = "NodeHealth{" +
                "host='" + host + '\'' +
                ", responseStatus='" + responseStatus + '\'';
        if (meteringDetails != null) {
            result += ", meteringDetails=" + meteringDetails;
        } else if (networkDetails != null) {
            result += ", networkDetails=" + networkDetails;
        } else {
            result += ", servicesDetails=" + servicesDetails;
        }
        result += '}';
        return result;
    }
}
