package com.sequenceiq.cloudbreak.core.flow2.stack.downscale

import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential
import com.sequenceiq.cloudbreak.cloud.model.CloudStack
import com.sequenceiq.cloudbreak.core.flow2.stack.StackContext
import com.sequenceiq.cloudbreak.domain.Stack

class StackScalingFlowContext(flowId: String, stack: Stack, cloudContext: CloudContext, cloudCredential: CloudCredential, cloudStack: CloudStack,
                              val instanceGroupName: String, val instanceIds: Set<String>, val adjustment: Int?) : StackContext(flowId, stack, cloudContext, cloudCredential, cloudStack)
