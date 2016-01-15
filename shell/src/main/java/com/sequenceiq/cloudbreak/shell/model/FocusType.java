package com.sequenceiq.cloudbreak.shell.model;

public enum FocusType {

    ROOT("");

    private final String prefix;

    private FocusType(String prefix) {
        this.prefix = prefix;
    }

    public String prefix() {
        return prefix;
    }
}
