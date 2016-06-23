package com.sequenceiq.cloudbreak.cloud.model

import com.sequenceiq.cloudbreak.cloud.model.generic.CloudTypes

/**
 * Types of disks of a platform

 * @see CloudTypes

 * @see DiskType
 */
class DiskTypes(diskTypes: Collection<DiskType>, defaultDiskType: DiskType, private val diskMapping: Map<String, VolumeParameterType>) : CloudTypes<DiskType>(diskTypes, defaultDiskType) {

    fun diskMapping(): Map<String, VolumeParameterType> {
        return diskMapping
    }
}
