package com.sequenceiq.cloudbreak.clusterproxy;

import java.util.Objects;

public class ReadConfigEndpoint {
    private String endpointId;

    private String status;

    private String endpoint;

    private String updatedTime;

    public String getEndpointId() {
        return endpointId;
    }

    public void setEndpointId(String endpointId) {
        this.endpointId = endpointId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(String updatedTime) {
        this.updatedTime = updatedTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ReadConfigEndpoint that = (ReadConfigEndpoint) o;

        return Objects.equals(endpointId, that.endpointId) &&
                Objects.equals(status, that.status) &&
                Objects.equals(endpoint, that.endpoint) &&
                Objects.equals(updatedTime, that.updatedTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(endpointId, status, endpoint, updatedTime);
    }

    @Override
    public String toString() {
        return "ReadConfigEndpoint{" +
                "endpointId='" + endpointId + '\'' +
                ", status='" + status + '\'' +
                ", endpoint='" + endpoint + '\'' +
                ", updatedTime='" + updatedTime + "\'}";
    }
}
