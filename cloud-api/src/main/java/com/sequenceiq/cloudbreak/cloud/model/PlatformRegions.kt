package com.sequenceiq.cloudbreak.cloud.model

import java.util.HashMap

class PlatformRegions {

    val regions: Map<Platform, Collection<Region>>
    val availabiltyZones: Map<Platform, Map<Region, List<AvailabilityZone>>>
    val defaultRegions: Map<Platform, Region>

    constructor(regions: Map<Platform, Collection<Region>>, availabiltyZones: Map<Platform, Map<Region, List<AvailabilityZone>>>,
                defaultRegions: Map<Platform, Region>) {
        this.regions = regions
        this.availabiltyZones = availabiltyZones
        this.defaultRegions = defaultRegions
    }

    constructor() {
        this.regions = HashMap<Platform, Collection<Region>>()
        this.availabiltyZones = HashMap<Platform, Map<Region, List<AvailabilityZone>>>()
        this.defaultRegions = HashMap<Platform, Region>()
    }
}
