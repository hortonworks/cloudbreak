package com.sequenceiq.cloudbreak.api.model

import java.util.HashMap

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
class PlatformVirtualMachinesJson : JsonEntity {

    var virtualMachines: Map<String, Collection<VmTypeJson>> = HashMap()
    var defaultVirtualMachines: Map<String, String> = HashMap()


}
