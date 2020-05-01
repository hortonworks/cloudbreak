package com.sequenceiq.freeipa.service.freeipa.user.model;

import java.util.Objects;

public class FmsGroup {

    private String name;

    public FmsGroup withName(String name) {
        this.name = name;
        return this;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        FmsGroup other = (FmsGroup) o;

        return Objects.equals(this.name, other.name);

    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "FmsGroup{"
                + "name='" + name + '\''
                + '}';
    }
}