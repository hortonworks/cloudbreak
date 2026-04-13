package com.sequenceiq.cloudbreak.cloud.gcp;

import static com.sequenceiq.cloudbreak.cloud.gcp.GcpDiskConstants.DEFAULT_DISK_MIN_SIZE;
import static com.sequenceiq.cloudbreak.cloud.gcp.GcpDiskConstants.EXTREME_DISK_MAX_SIZE;
import static com.sequenceiq.cloudbreak.cloud.gcp.GcpDiskConstants.HYPERDISK_BALANCED_MAX_SIZE_GB;
import static com.sequenceiq.cloudbreak.cloud.gcp.GcpDiskConstants.HYPERDISK_BALANCED_MIN_SIZE_GB;
import static com.sequenceiq.cloudbreak.cloud.gcp.GcpDiskConstants.HYPERDISK_EXTREME_MAX_SIZE_GB;
import static com.sequenceiq.cloudbreak.cloud.gcp.GcpDiskConstants.HYPERDISK_EXTREME_MIN_SIZE_GB;
import static com.sequenceiq.cloudbreak.cloud.gcp.GcpDiskConstants.HYPERDISK_THROUGHPUT_MAX_SIZE_GB;
import static com.sequenceiq.cloudbreak.cloud.gcp.GcpDiskConstants.HYPERDISK_THROUGHPUT_MIN_SIZE_GB;
import static com.sequenceiq.cloudbreak.cloud.gcp.GcpDiskConstants.UNDEFINED_DISK_SIZE;

import java.util.Arrays;

import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterType;

public enum GcpDiskType {
    SSD("pd-ssd", "Solid-State Persistent Disk (SSD)", VolumeParameterType.SSD, DEFAULT_DISK_MIN_SIZE, UNDEFINED_DISK_SIZE),
    BALANCED("pd-balanced", "Balanced Persistent Disk (BALANCED)", VolumeParameterType.MAGNETIC, DEFAULT_DISK_MIN_SIZE, UNDEFINED_DISK_SIZE),
    EXTREME("pd-extreme", "Extreme Persistent Disk (EXTREME)", VolumeParameterType.SSD, DEFAULT_DISK_MIN_SIZE, EXTREME_DISK_MAX_SIZE),
    HDD("pd-standard", "Standard Persistent Disk (HDD)", VolumeParameterType.MAGNETIC, DEFAULT_DISK_MIN_SIZE, UNDEFINED_DISK_SIZE),
    LOCAL_SSD("local-ssd", "Scratch Disk (SSD)", VolumeParameterType.LOCAL_SSD, UNDEFINED_DISK_SIZE, UNDEFINED_DISK_SIZE),
    HYPERDISK_EXTREME("hyperdisk-extreme", "Hyperdisk Extreme (HYPERDISK_EXTREME)", VolumeParameterType.HYPERDISK_EXTREME,
            HYPERDISK_EXTREME_MIN_SIZE_GB, HYPERDISK_EXTREME_MAX_SIZE_GB),
    HYPERDISK_BALANCED("hyperdisk-balanced", "Hyperdisk Balanced (HYPERDISK_BALANCED)", VolumeParameterType.HYPERDISK_BALANCED,
            HYPERDISK_BALANCED_MIN_SIZE_GB, HYPERDISK_BALANCED_MAX_SIZE_GB),
    HYPERDISK_THROUGHPUT("hyperdisk-throughput", "Hyperdisk Throughput (HYPERDISK_THROUGHPUT)", VolumeParameterType.HYPERDISK_THROUGHPUT,
            HYPERDISK_THROUGHPUT_MIN_SIZE_GB, HYPERDISK_THROUGHPUT_MAX_SIZE_GB);

    private final String value;

    private final String displayName;

    private final VolumeParameterType volumeParameterType;

    private final int minimumSize;

    private final int maximumSize;

    GcpDiskType(String value, String displayName, VolumeParameterType volumeParameterType, int minimumSize, int maximumSize) {
        this.value = value;
        this.displayName = displayName;
        this.volumeParameterType = volumeParameterType;
        this.minimumSize = minimumSize;
        this.maximumSize = maximumSize;
    }

    public static GcpDiskType findByValue(String value) {
        return Arrays.stream(GcpDiskType.values())
                .filter(diskType -> diskType.value.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No GCP disk type found for value: " + value));
    }

    public static String getUrl(String projectId, String zone, String volumeId) {
        return String.format("https://www.googleapis.com/compute/v1/projects/%s/zones/%s/diskTypes/%s", projectId, zone, volumeId);
    }

    public String value() {
        return value;
    }

    public String displayName() {
        return displayName;
    }

    public VolumeParameterType getVolumeParameterType() {
        return volumeParameterType;
    }

    public int getMinimumSize() {
        return minimumSize;
    }

    public int getMaximumSize() {
        return maximumSize;
    }

    public String getUrl(String projectId, String zone) {
        return getUrl(projectId, zone, value);
    }
}
