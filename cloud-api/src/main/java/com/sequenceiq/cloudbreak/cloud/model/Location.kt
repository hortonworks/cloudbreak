package com.sequenceiq.cloudbreak.cloud.model

class Location private constructor(val region: Region, val availabilityZone: AvailabilityZone) {
    companion object {

        fun location(region: Region, availabilityZone: AvailabilityZone): Location {
            return Location(region, availabilityZone)
        }

        fun location(region: Region): Location {
            return Location(region, null)
        }
    }
}
