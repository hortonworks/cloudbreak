package com.sequenceiq.cloudbreak.cloud.model

import com.sequenceiq.cloudbreak.cloud.model.generic.CloudTypes

/**
 * Regions of a platform

 * @see CloudTypes

 * @see Region
 */
class Regions(types: Collection<Region>, defaultType: Region) : CloudTypes<Region>(types, defaultType)
