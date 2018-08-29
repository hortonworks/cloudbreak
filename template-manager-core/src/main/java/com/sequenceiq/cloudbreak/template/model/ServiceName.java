package com.sequenceiq.cloudbreak.template.model;

import java.util.Objects;

public class ServiceName {

    private final String name;

    private ServiceName(String name) {
        this.name = name;
    }

    public static ServiceName serviceName(String name) {
        return new ServiceName(name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ServiceName that = (ServiceName) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {

        return Objects.hash(name);
    }
}
