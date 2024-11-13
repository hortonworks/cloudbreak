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
    UNKNOWN;

    public boolean isDownloaded() {
        return DOWNLOADED.equals(this)
                || DISTRIBUTING.equals(this)
                || DISTRIBUTED.equals(this)
                || UNDISTRIBUTING.equals(this)
                || ACTIVATING.equals(this)
                || ACTIVATED.equals(this);
    }

    public boolean isDistributed() {
        return DISTRIBUTED.equals(this)
                || UNDISTRIBUTING.equals(this)
                || ACTIVATING.equals(this)
                || ACTIVATED.equals(this);
    }

    public boolean isActivated() {
        return ACTIVATED.equals(this);
    }
}
