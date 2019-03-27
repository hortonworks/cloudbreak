package com.sequenceiq.cloudbreak.cmtemplate.generator.configuration.domain;

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
}
