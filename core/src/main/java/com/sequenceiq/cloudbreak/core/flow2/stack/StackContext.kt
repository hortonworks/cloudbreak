package com.sequenceiq.cloudbreak.core.flow2.stack

import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential
import com.sequenceiq.cloudbreak.cloud.model.CloudStack
import com.sequenceiq.cloudbreak.core.flow2.CommonContext
import com.sequenceiq.cloudbreak.domain.Stack

open class StackContext(flowId: String, val stack: Stack, val cloudContext: CloudContext, val cloudCredential: CloudCredential, val cloudStack: CloudStack) : CommonContext(flowId)
