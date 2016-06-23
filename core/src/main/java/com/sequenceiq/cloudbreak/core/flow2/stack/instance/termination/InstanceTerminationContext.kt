package com.sequenceiq.cloudbreak.core.flow2.stack.instance.termination

import com.google.common.collect.ImmutableList
import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance
import com.sequenceiq.cloudbreak.cloud.model.CloudResource
import com.sequenceiq.cloudbreak.cloud.model.CloudStack
import com.sequenceiq.cloudbreak.core.flow2.CommonContext
import com.sequenceiq.cloudbreak.domain.InstanceMetaData
import com.sequenceiq.cloudbreak.domain.Stack

class InstanceTerminationContext(flowId: String, val stack: Stack, val cloudContext: CloudContext, val cloudCredential: CloudCredential,
                                 val cloudStack: CloudStack, cloudResources: List<CloudResource>, val cloudInstance: CloudInstance, val instanceMetaData: InstanceMetaData) : CommonContext(flowId) {
    val cloudResources: List<CloudResource>


    init {
        this.cloudResources = ImmutableList.copyOf(cloudResources)
    }
}
