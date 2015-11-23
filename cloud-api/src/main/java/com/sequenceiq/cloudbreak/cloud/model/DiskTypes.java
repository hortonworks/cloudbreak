package com.sequenceiq.cloudbreak.cloud.model;

import java.util.Collection;

import com.sequenceiq.cloudbreak.cloud.model.generic.CloudTypes;

/**
 * Types of disks of a platform
 *
 * @see CloudTypes
 * @see DiskType
 */
public class DiskTypes extends CloudTypes<DiskType> {
    public DiskTypes(Collection<DiskType> diskTypes, DiskType defaultDiskType) {
        super(diskTypes, defaultDiskType);
    }
}
