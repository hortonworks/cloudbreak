package com.sequenceiq.cloudbreak.cloud.model

import com.sequenceiq.cloudbreak.cloud.model.generic.CloudTypes

/**
 * Virtual machine types of a platform

 * @see CloudTypes

 * @see VmType
 */
class VmTypes(types: Collection<VmType>, defaultType: VmType) : CloudTypes<VmType>(types, defaultType)
