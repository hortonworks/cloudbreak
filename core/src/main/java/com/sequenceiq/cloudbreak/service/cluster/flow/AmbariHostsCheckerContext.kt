package com.sequenceiq.cloudbreak.service.cluster.flow

import com.sequenceiq.ambari.client.AmbariClient
import com.sequenceiq.cloudbreak.domain.HostMetadata
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.service.StackContext

class AmbariHostsCheckerContext(stack: Stack, val ambariClient: AmbariClient, val hostsInCluster: Set<HostMetadata>, val hostCount: Int) : StackContext(stack)
