package com.sequenceiq.cloudbreak.core.flow2.cluster.downscale

import com.sequenceiq.cloudbreak.core.flow2.cluster.ClusterScaleContext
import com.sequenceiq.cloudbreak.domain.Stack

internal class ClusterUpdateMetadataContext(flowId: String, stack: Stack, hostGroupName: String, val hostNames: Set<String>) : ClusterScaleContext(flowId, stack, hostGroupName)
