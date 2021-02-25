package com.sequenceiq.common.api.node.status.response;

public class NodeReport {

    private String host;

    private String responseStatus;

    private MeteringDetails meteringDetails;

    private NetworkDetails networkDetails;

    private ServiceDetails serviceDetails;

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

    public ServiceDetails getServiceDetails() {
        return serviceDetails;
    }

    public void setServiceDetails(ServiceDetails serviceDetails) {
        this.serviceDetails = serviceDetails;
    }

    @Override
    public String toString() {
        return "NodeReport{" +
                "host='" + host + '\'' +
                ", responseStatus='" + responseStatus + '\'' +
                ", meteringDetails=" + meteringDetails +
                ", networkDetails=" + networkDetails +
                ", serviceDetails=" + serviceDetails +
                '}';
    }
}
