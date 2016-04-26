package com.sequenceiq.cloudbreak.orchestrator.onhost.domain;

public class CbBootResponse {

    private String status;
    private String address;
    private int statusCode;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("CbBootResponse{");
        sb.append("status='").append(status).append('\'');
        sb.append(", address='").append(address).append('\'');
        sb.append(", statusCode=").append(statusCode);
        sb.append('}');
        return sb.toString();
    }
}
