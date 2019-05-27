package com.sequenceiq.cloudbreak.cmtemplate.generator.configuration.domain;

import java.util.Objects;

public class StackVersion {

    private String stackType;

    private String version;

    public String getStackType() {
        return stackType;
    }

    public void setStackType(String stackType) {
        this.stackType = stackType;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        StackVersion that = (StackVersion) o;
        return Objects.equals(stackType, that.stackType) && Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stackType, version);
    }
}
