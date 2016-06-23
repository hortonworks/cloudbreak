package com.sequenceiq.cloudbreak.cloud.model

import com.sequenceiq.cloudbreak.cloud.model.generic.StringType

class AvailabilityZone(value: String) : StringType(value) {
    companion object {

        fun availabilityZone(value: String): AvailabilityZone {
            return AvailabilityZone(value)
        }
    }

}
