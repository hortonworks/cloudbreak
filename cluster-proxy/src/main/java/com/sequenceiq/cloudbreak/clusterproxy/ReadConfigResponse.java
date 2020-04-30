package com.sequenceiq.cloudbreak.clusterproxy;

import java.util.List;
import java.util.Objects;

public class ReadConfigResponse {
    private String crn;

    private List<ReadConfigService> services;

    public String getCrn() {
        return crn;
    }

    public void setCrn(String crn) {
        this.crn = crn;
    }

    public List<ReadConfigService> getServices() {
        return services;
    }

    public void setServices(List<ReadConfigService> services) {
        this.services = services;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ReadConfigResponse that = (ReadConfigResponse) o;

        return Objects.equals(crn, that.crn) &&
                Objects.equals(services, that.services);
    }

    @Override
    public int hashCode() {
        return Objects.hash(crn, services);
    }

    @Override
    public String toString() {
        return "ReadConfigResponse{crn='" + crn + '\'' + ", services=" + services + '}';
    }
}
