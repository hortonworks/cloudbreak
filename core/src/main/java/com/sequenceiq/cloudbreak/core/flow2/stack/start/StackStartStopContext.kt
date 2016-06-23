package com.sequenceiq.cloudbreak.core.flow2.stack.start

import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential
import com.sequenceiq.cloudbreak.core.flow2.CommonContext
import com.sequenceiq.cloudbreak.domain.InstanceMetaData
import com.sequenceiq.cloudbreak.domain.Stack

class StackStartStopContext(flowId: String, val stack: Stack, val instanceMetaData: List<InstanceMetaData>,
                            val cloudContext: CloudContext, val cloudCredential: CloudCredential) : CommonContext(flowId)
