package com.sequenceiq.cloudbreak.service.cluster;

public class EmbeddedDatabaseInfo {
    private final boolean embeddedDatabaseOnAttachedDiskEnabled;

    private final int attachedDisksCount;

    public EmbeddedDatabaseInfo(boolean embeddedDatabaseOnAttachedDiskEnabled, int attachedDisksCount) {
        this.embeddedDatabaseOnAttachedDiskEnabled = embeddedDatabaseOnAttachedDiskEnabled;
        this.attachedDisksCount = attachedDisksCount;
    }

    public boolean isEmbeddedDatabaseOnAttachedDiskEnabled() {
        return embeddedDatabaseOnAttachedDiskEnabled;
    }

    public int getAttachedDisksCount() {
        return attachedDisksCount;
    }
}
