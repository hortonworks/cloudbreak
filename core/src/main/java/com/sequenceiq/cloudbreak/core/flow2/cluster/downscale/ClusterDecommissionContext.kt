package com.sequenceiq.cloudbreak.core.flow2.cluster.downscale

import com.sequenceiq.cloudbreak.core.flow2.cluster.ClusterScaleContext
import com.sequenceiq.cloudbreak.domain.Stack

internal class ClusterDecommissionContext(flowId: String, stack: Stack, hostGroupName: String, val scalingAdjustment: Int?) : ClusterScaleContext(flowId, stack, hostGroupName)
