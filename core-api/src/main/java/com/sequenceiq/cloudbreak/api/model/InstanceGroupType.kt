package com.sequenceiq.cloudbreak.api.model

enum class InstanceGroupType private constructor(val fixedNodeCount: Int?) {
    GATEWAY(1), CORE(0);


    companion object {

        fun isGateway(type: InstanceGroupType): Boolean {
            return GATEWAY == type
        }

        fun isCoreGroup(type: InstanceGroupType): Boolean {
            return CORE == type
        }
    }
}