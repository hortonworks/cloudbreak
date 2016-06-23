package com.sequenceiq.cloudbreak.core.flow2.cluster

import com.sequenceiq.cloudbreak.core.flow2.CommonContext
import com.sequenceiq.cloudbreak.domain.Cluster
import com.sequenceiq.cloudbreak.domain.Stack

open class ClusterContext(flowId: String, val stack: Stack, val cluster: Cluster) : CommonContext(flowId)
