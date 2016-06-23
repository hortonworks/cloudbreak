package com.sequenceiq.cloudbreak.cloud.model

import com.sequenceiq.cloudbreak.cloud.model.generic.StringType

class DiskType private constructor(diskType: String) : StringType(diskType) {
    companion object {

        fun diskType(diskType: String): DiskType {
            return DiskType(diskType)
        }
    }
}
