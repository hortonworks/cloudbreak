package com.sequenceiq.cloudbreak.cloud.model

import java.util.HashMap

class PlatformVirtualMachines {

    val virtualMachines: Map<Platform, Collection<VmType>>
    val defaultVirtualMachines: Map<Platform, VmType>

    constructor(virtualMachines: Map<Platform, Collection<VmType>>, defaultVirtualMachines: Map<Platform, VmType>) {
        this.virtualMachines = virtualMachines
        this.defaultVirtualMachines = defaultVirtualMachines
    }

    constructor() {
        this.virtualMachines = HashMap<Platform, Collection<VmType>>()
        this.defaultVirtualMachines = HashMap<Platform, VmType>()
    }
}
