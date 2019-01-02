package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base;

public enum StackResponseEntries {

    HARDWARE_INFO("hardware_info"),
    EVENTS("events");

    private final String entryName;

    StackResponseEntries(String entryName) {
        this.entryName = entryName;
    }

    public String getEntryName() {
        return entryName;
    }
}
