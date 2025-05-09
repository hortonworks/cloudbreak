package com.sequenceiq.cloudbreak.clusterproxy;

import java.util.Objects;

public class ReadConfigEndpoint {

    private String endpointId;

    private String endpoint;

    public String getEndpointId() {
        return endpointId;
    }

    public void setEndpointId(String endpointId) {
        this.endpointId = endpointId;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
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
                Objects.equals(endpoint, that.endpoint);
    }

    @Override
    public int hashCode() {
        return Objects.hash(endpointId, endpoint);
    }

    @Override
    public String toString() {
        return "ReadConfigEndpoint{" +
                "endpointId='" + endpointId + '\'' +
                ", endpoint='" + endpoint + '\'' +
                '}';
    }
}
