package com.sequenceiq.cloudbreak.controller.validation.blueprint

class StackServiceComponentDescriptors(private val stackServiceComponentDescriptorMap: Map<String, StackServiceComponentDescriptor>) {

    operator fun get(name: String): StackServiceComponentDescriptor {
        return stackServiceComponentDescriptorMap[name]
    }
}
