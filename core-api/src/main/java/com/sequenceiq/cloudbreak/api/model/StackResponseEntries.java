package com.sequenceiq.cloudbreak.api.model;

public enum StackResponseEntries {

    HARDWARE_INFO("hardware_info"),
    USAGES("usages"),
    EVENTS("events");

    private String entryName;

    StackResponseEntries(String entryName) {
        this.entryName = entryName;
    }

    public String getEntryName() {
        return entryName;
    }
}
