package com.sequenceiq.cloudbreak.api.model.stack;

public enum StackResponseEntries {

    HARDWARE_INFO("hardware_info");

    private final String entryName;

    StackResponseEntries(String entryName) {
        this.entryName = entryName;
    }

    public String getEntryName() {
        return entryName;
    }
}
