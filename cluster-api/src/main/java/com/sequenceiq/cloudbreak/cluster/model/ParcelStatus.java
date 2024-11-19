package com.sequenceiq.cloudbreak.cluster.model;

public enum ParcelStatus {

    AVAILABLE_REMOTELY,
    DOWNLOADING,
    DOWNLOADED,
    DISTRIBUTING,
    DISTRIBUTED,
    UNDISTRIBUTING,
    ACTIVATING,
    ACTIVATED,
    UNAVAILABLE,
    UNKNOWN
}
