package com.sequenceiq.cloudbreak.core.flow2.cluster

import com.sequenceiq.cloudbreak.core.flow2.CommonContext
import com.sequenceiq.cloudbreak.domain.Stack

open class ClusterScaleContext(flowId: String, val stack: Stack, val hostGroupName: String) : CommonContext(flowId)
