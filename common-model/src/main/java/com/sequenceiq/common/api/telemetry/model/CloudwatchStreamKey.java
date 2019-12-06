package com.sequenceiq.common.api.telemetry.model;

/**
 * For logging, Cloudwatch streams will be created based on this key
 */
public enum CloudwatchStreamKey {

    HOSTNAME("hostname"),
    COMPONENT("component");

    private String value;

    CloudwatchStreamKey(String value) {
        this.value = value;
    }

    public String value() {
        return this.value;
    }
}
