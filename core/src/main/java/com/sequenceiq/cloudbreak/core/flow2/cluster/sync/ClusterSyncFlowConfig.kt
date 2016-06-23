package com.sequenceiq.cloudbreak.core.flow2.cluster.sync

import com.sequenceiq.cloudbreak.core.flow2.cluster.sync.ClusterSyncEvent.CLUSTER_SYNC_EVENT
import com.sequenceiq.cloudbreak.core.flow2.cluster.sync.ClusterSyncEvent.FAILURE_EVENT
import com.sequenceiq.cloudbreak.core.flow2.cluster.sync.ClusterSyncEvent.FAIL_HANDLED_EVENT
import com.sequenceiq.cloudbreak.core.flow2.cluster.sync.ClusterSyncEvent.FINALIZED_EVENT
import com.sequenceiq.cloudbreak.core.flow2.cluster.sync.ClusterSyncState.CLUSTER_SYNC_FAILED_STATE
import com.sequenceiq.cloudbreak.core.flow2.cluster.sync.ClusterSyncState.CLUSTER_SYNC_STATE
import com.sequenceiq.cloudbreak.core.flow2.cluster.sync.ClusterSyncState.FINAL_STATE
import com.sequenceiq.cloudbreak.core.flow2.cluster.sync.ClusterSyncState.INIT_STATE

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.core.flow2.config.AbstractFlowConfiguration

@Component
class ClusterSyncFlowConfig : AbstractFlowConfiguration<ClusterSyncState, ClusterSyncEvent>(ClusterSyncState::class.java, ClusterSyncEvent::class.java) {

    protected override val transitions: List<AbstractFlowConfiguration.Transition<ClusterSyncState, ClusterSyncEvent>>
        get() = TRANSITIONS

    protected override val edgeConfig: AbstractFlowConfiguration.FlowEdgeConfig<ClusterSyncState, ClusterSyncEvent>
        get() = EDGE_CONFIG

    override val events: Array<ClusterSyncEvent>
        get() = ClusterSyncEvent.values()

    override val initEvents: Array<ClusterSyncEvent>
        get() = arrayOf(CLUSTER_SYNC_EVENT)

    companion object {
        private val TRANSITIONS = Transition.Builder<ClusterSyncState, ClusterSyncEvent>().from(INIT_STATE).to(CLUSTER_SYNC_STATE).event(CLUSTER_SYNC_EVENT).noFailureEvent().from(CLUSTER_SYNC_STATE).to(ClusterSyncState.CLUSTER_SYNC_FINISHED_STATE).event(ClusterSyncEvent.CLUSTER_SYNC_FINISHED_EVENT).failureEvent(ClusterSyncEvent.CLUSTER_SYNC_FINISHED_FAILURE_EVENT).from(ClusterSyncState.CLUSTER_SYNC_FINISHED_STATE).to(FINAL_STATE).event(FINALIZED_EVENT).failureEvent(FAILURE_EVENT).build()

        private val EDGE_CONFIG = AbstractFlowConfiguration.FlowEdgeConfig(INIT_STATE, FINAL_STATE,
                CLUSTER_SYNC_FAILED_STATE, FAIL_HANDLED_EVENT)
    }
}
