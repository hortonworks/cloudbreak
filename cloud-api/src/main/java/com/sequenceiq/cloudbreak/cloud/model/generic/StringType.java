package com.sequenceiq.cloudbreak.cloud.model.generic;

import java.util.Objects;

public abstract class StringType {

    private final String value;

    protected StringType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    @Override
    public String toString() {
        return "StringType{value='" + value + "\'}";
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !Objects.equals(getClass(), o.getClass())) {
            return false;
        } else if (this == o) {
            return true;
        }
        StringType that = (StringType) o;
        return Objects.equals(value, that.value);

    }

    @Override
    public int hashCode() {
        return value != null ? value.hashCode() : 0;
    }
}
