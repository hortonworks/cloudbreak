package com.sequenceiq.common.api.backup.model;

/**
 * For logging, Cloudwatch streams will be created based on this key
 */
public enum BackupCloudwatchStreamKey {

    HOSTNAME("hostname"),
    COMPONENT("component");

    private String value;

    BackupCloudwatchStreamKey(String value) {
        this.value = value;
    }

    public String value() {
        return this.value;
    }
}
