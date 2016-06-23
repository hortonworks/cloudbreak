package com.sequenceiq.cloudbreak.core.flow2.stack.termination

import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential
import com.sequenceiq.cloudbreak.cloud.model.CloudResource
import com.sequenceiq.cloudbreak.cloud.model.CloudStack
import com.sequenceiq.cloudbreak.core.flow2.stack.StackContext
import com.sequenceiq.cloudbreak.domain.Stack

class StackTerminationContext(flowId: String, stack: Stack, cloudContext: CloudContext, cloudCredential: CloudCredential, cloudStack: CloudStack,
                              val cloudResources: List<CloudResource>) : StackContext(flowId, stack, cloudContext, cloudCredential, cloudStack)
