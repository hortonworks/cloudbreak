package com.sequenceiq.cloudbreak.cloud.model

import com.google.common.collect.ImmutableList
import com.sequenceiq.cloudbreak.api.model.InstanceGroupType

class Group(val name: String, val type: InstanceGroupType, instances: List<CloudInstance>) {
    val instances: List<CloudInstance>

    init {
        this.instances = ImmutableList.copyOf(instances)
    }

}
