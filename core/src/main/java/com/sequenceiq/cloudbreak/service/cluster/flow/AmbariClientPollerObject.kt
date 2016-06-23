package com.sequenceiq.cloudbreak.service.cluster.flow

import com.sequenceiq.ambari.client.AmbariClient
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.service.StackContext

open class AmbariClientPollerObject(stack: Stack, val ambariClient: AmbariClient) : StackContext(stack)
