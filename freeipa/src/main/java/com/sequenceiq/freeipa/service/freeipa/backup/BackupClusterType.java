package com.sequenceiq.freeipa.service.freeipa.backup;

public enum BackupClusterType {
    FREEIPA("freeipa");

    private String value;

    BackupClusterType(String value) {
        this.value = value;
    }

    public String value() {
        return this.value;
    }
}
