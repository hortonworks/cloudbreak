package com.sequenceiq.cloudbreak.core.flow2.cluster.upscale

import com.sequenceiq.cloudbreak.core.flow2.cluster.ClusterContext
import com.sequenceiq.cloudbreak.domain.Stack

class ClusterUpscaleContext(flowId: String, stack: Stack, val hostGroupName: String, val adjustment: Int?) : ClusterContext(flowId, stack, stack.cluster)
