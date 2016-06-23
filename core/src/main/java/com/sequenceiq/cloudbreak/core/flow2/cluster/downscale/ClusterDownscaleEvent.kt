package com.sequenceiq.cloudbreak.core.flow2.cluster.downscale

import com.sequenceiq.cloudbreak.core.flow2.FlowEvent
import com.sequenceiq.cloudbreak.core.flow2.FlowTriggers
import com.sequenceiq.cloudbreak.reactor.api.event.EventSelectorUtil
import com.sequenceiq.cloudbreak.reactor.api.event.resource.DecommissionResult
import com.sequenceiq.cloudbreak.reactor.api.event.resource.UpdateInstanceMetadataResult

enum class ClusterDownscaleEvent private constructor(private val stringRepresentation: String) : FlowEvent {
    DECOMMISSION_EVENT(FlowTriggers.CLUSTER_DOWNSCALE_TRIGGER_EVENT),
    DECOMMISSION_FINISHED_EVENT(EventSelectorUtil.selector(DecommissionResult::class.java)),
    DECOMMISSION_FAILED_EVENT(EventSelectorUtil.failureSelector(DecommissionResult::class.java)),
    UPDATE_METADATA_FINISHED_EVENT(EventSelectorUtil.selector(UpdateInstanceMetadataResult::class.java)),
    UPDATE_METADATA_FAILED_EVENT(EventSelectorUtil.failureSelector(UpdateInstanceMetadataResult::class.java)),
    FINALIZED_EVENT("CLUSTERDOWNSCALEFINALIZEDEVENT"),
    FAILURE_EVENT("CLUSTERDOWNSCALEFAILUREEVENT"),
    FAIL_HANDLED_EVENT("CLUSTERDOWNSCALEFAILHANDLEDEVENT");

    override fun stringRepresentation(): String {
        return stringRepresentation
    }
}
