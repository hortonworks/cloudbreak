package com.sequenceiq.cloudbreak.core.flow2.stack

import com.sequenceiq.cloudbreak.core.flow2.CommonContext
import com.sequenceiq.cloudbreak.domain.Stack

class StackFailureContext(flowId: String, val stack: Stack) : CommonContext(flowId)
