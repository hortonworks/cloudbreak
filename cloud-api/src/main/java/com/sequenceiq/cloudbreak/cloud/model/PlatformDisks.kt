package com.sequenceiq.cloudbreak.cloud.model

import java.util.HashMap

class PlatformDisks {
    var diskTypes: Map<Platform, Collection<DiskType>>? = null
        private set
    var defaultDisks: Map<Platform, DiskType>? = null
        private set
    var diskMappings: Map<Platform, Map<String, VolumeParameterType>>? = null
        private set

    constructor(diskTypes: Map<Platform, Collection<DiskType>>, defaultDisks: Map<Platform, DiskType>,
                diskMappings: Map<Platform, Map<String, VolumeParameterType>>) {
        this.diskTypes = diskTypes
        this.defaultDisks = defaultDisks
        this.diskMappings = diskMappings
    }

    constructor() {
        this.diskTypes = HashMap<Platform, Collection<DiskType>>()
        this.defaultDisks = HashMap<Platform, DiskType>()
        this.diskMappings = HashMap<Platform, Map<String, VolumeParameterType>>()
    }
}
