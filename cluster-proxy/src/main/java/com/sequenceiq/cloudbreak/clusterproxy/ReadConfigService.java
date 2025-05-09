package com.sequenceiq.cloudbreak.clusterproxy;

import java.util.List;
import java.util.Objects;

public class ReadConfigService {

    private String name;

    private List<ReadConfigEndpoint> endpoints;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<ReadConfigEndpoint> getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(List<ReadConfigEndpoint> endpoints) {
        this.endpoints = endpoints;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ReadConfigService that = (ReadConfigService) o;

        return Objects.equals(name, that.name) &&
                Objects.equals(endpoints, that.endpoints);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, endpoints);
    }

    @Override
    public String toString() {
        return "ReadConfigEndpoint{name='" + name + '\'' + ", endpoints=" + endpoints + '}';
    }
}
