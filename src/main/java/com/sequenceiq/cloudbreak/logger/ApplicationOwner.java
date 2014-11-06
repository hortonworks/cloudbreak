package com.sequenceiq.cloudbreak.logger;

public enum ApplicationOwner {

    CLOUDBREAK("cloudbreak");

    private final String value;

    ApplicationOwner(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return this.value;
    }
}
