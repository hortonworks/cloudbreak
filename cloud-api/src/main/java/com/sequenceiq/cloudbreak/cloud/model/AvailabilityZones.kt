package com.sequenceiq.cloudbreak.cloud.model

import com.google.common.collect.Lists

/**
 * Availability zones of [Region] of a [Platform]

 * @see Region,Platform,AvailabilityZone
 */
class AvailabilityZones(val all: Map<Region, List<AvailabilityZone>>) {

    val allAvailabilityZone: List<AvailabilityZone>
        get() {
            val result = Lists.newArrayList<AvailabilityZone>()
            for (entry in all.entries) {
                result.addAll(entry.value)
            }
            return result
        }

    fun getAvailabilityZonesByRegion(region: Region): List<AvailabilityZone> {
        return all[region]
    }
}
