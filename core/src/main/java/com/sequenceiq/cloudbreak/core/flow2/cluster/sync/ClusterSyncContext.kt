package com.sequenceiq.cloudbreak.core.flow2.cluster.sync

import com.sequenceiq.cloudbreak.core.flow2.CommonContext
import com.sequenceiq.cloudbreak.domain.Stack

class ClusterSyncContext(flowId: String, val stack: Stack) : CommonContext(flowId)
