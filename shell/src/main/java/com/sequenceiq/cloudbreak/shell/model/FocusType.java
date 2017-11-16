package com.sequenceiq.cloudbreak.shell.model;

public enum FocusType {

    YARN("cloudbreak-shell:yarn"),
    ROOT("");

    private final String prefix;

    FocusType(String prefix) {
        this.prefix = prefix;
    }

    public String prefix() {
        return prefix;
    }
}
