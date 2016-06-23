package com.sequenceiq.cloudbreak.core.flow2.cluster.sync

import com.sequenceiq.cloudbreak.core.flow2.FlowEvent
import com.sequenceiq.cloudbreak.core.flow2.FlowTriggers
import com.sequenceiq.cloudbreak.reactor.api.event.EventSelectorUtil
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ClusterSyncResult

enum class ClusterSyncEvent private constructor(private val stringRepresentation: String) : FlowEvent {
    CLUSTER_SYNC_EVENT(FlowTriggers.CLUSTER_SYNC_TRIGGER_EVENT),
    CLUSTER_SYNC_FINISHED_EVENT(EventSelectorUtil.selector(ClusterSyncResult::class.java)),
    CLUSTER_SYNC_FINISHED_FAILURE_EVENT(EventSelectorUtil.failureSelector(ClusterSyncResult::class.java)),

    FINALIZED_EVENT("CLUSTERSYNCFINALIZEDEVENT"),
    FAILURE_EVENT("CLUSTERSYNCFAILUREEVENT"),
    FAIL_HANDLED_EVENT("CLUSTERSYNCFAILHANDLEDEVENT");

    override fun stringRepresentation(): String {
        return stringRepresentation
    }
}
