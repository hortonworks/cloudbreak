package com.sequenceiq.cloudbreak.cloud.model;

import java.util.Collection;
import java.util.Map;

import com.sequenceiq.cloudbreak.cloud.model.generic.CloudTypes;

/**
 * Types of disks of a platform
 *
 * @see CloudTypes
 * @see DiskType
 */
public class DiskTypes extends CloudTypes<DiskType> {
    private final Map<String, VolumeParameterType> diskMapping;

    public DiskTypes(Collection<DiskType> diskTypes, DiskType defaultDiskType, Map<String, VolumeParameterType> diskMapping) {
        super(diskTypes, defaultDiskType);
        this.diskMapping = diskMapping;
    }

    public Map<String, VolumeParameterType> diskMapping() {
        return diskMapping;
    }
}
