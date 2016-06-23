package com.sequenceiq.cloudbreak.core.flow2.cluster

import com.sequenceiq.cloudbreak.reactor.api.event.HostGroupPayload

open class ClusterScaleFailedPayload(override val stackId: Long?, override val hostGroupName: String, val errorDetails: Exception) : HostGroupPayload
