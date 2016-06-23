package com.sequenceiq.cloudbreak.service.cluster.flow

import com.sequenceiq.ambari.client.AmbariClient
import com.sequenceiq.cloudbreak.domain.Stack

class AmbariHostsWithNames(stack: Stack, ambariClient: AmbariClient, val hostNames: List<String>) : AmbariClientPollerObject(stack, ambariClient)
