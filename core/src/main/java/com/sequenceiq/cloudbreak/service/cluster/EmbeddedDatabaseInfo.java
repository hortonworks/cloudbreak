package com.sequenceiq.cloudbreak.service.cluster;

public class EmbeddedDatabaseInfo {
    private final boolean embeddedDatabaseOnAttachedDiskEnabled;

    private final int attachedDisksCount;

    public EmbeddedDatabaseInfo(int attachedDisksCount) {
        this.embeddedDatabaseOnAttachedDiskEnabled = attachedDisksCount > 0;
        this.attachedDisksCount = attachedDisksCount;
    }

    public boolean isEmbeddedDatabaseOnAttachedDiskEnabled() {
        return embeddedDatabaseOnAttachedDiskEnabled;
    }

    public int getAttachedDisksCount() {
        return attachedDisksCount;
    }
}
