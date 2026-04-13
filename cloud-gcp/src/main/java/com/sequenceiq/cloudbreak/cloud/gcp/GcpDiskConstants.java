package com.sequenceiq.cloudbreak.cloud.gcp;

public class GcpDiskConstants {
    public static final int DEFAULT_DISK_MIN_SIZE = 10;

    public static final int UNDEFINED_DISK_SIZE = -1;

    public static final int EXTREME_DISK_MAX_SIZE = 1500;

    public static final int HYPERDISK_BALANCED_MIN_SIZE_GB = 10;

    public static final int HYPERDISK_BALANCED_MAX_SIZE_GB = 65536;

    public static final int HYPERDISK_EXTREME_MIN_SIZE_GB = 64;

    public static final int HYPERDISK_EXTREME_MAX_SIZE_GB = 65536;

    public static final int HYPERDISK_THROUGHPUT_MIN_SIZE_GB = 2048;

    public static final int HYPERDISK_THROUGHPUT_MAX_SIZE_GB = 32768;

    private GcpDiskConstants() {
    }
}
