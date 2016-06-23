package com.sequenceiq.cloudbreak.core.flow2.cluster.sync

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.statemachine.action.Action

import com.sequenceiq.cloudbreak.cloud.event.Selectable
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ClusterSyncRequest
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ClusterSyncResult

@Configuration
class ClusterSyncActions {

    @Bean(name = "CLUSTER_SYNC_STATE")
    fun syncCluster(): Action<Any, Any> {
        return object : AbstractClusterSyncAction<StackEvent>(StackEvent::class.java) {
            @Throws(Exception::class)
            override fun doExecute(context: ClusterSyncContext, payload: StackEvent, variables: Map<Any, Any>) {
                sendEvent(context)
            }

            override fun createRequest(context: ClusterSyncContext): Selectable {
                return ClusterSyncRequest(context.stack.id)
            }
        }
    }

    @Bean(name = "CLUSTER_SYNC_FINISHED_STATE")
    fun finishSyncCluster(): Action<Any, Any> {
        return object : AbstractClusterSyncAction<ClusterSyncResult>(ClusterSyncResult::class.java) {
            @Throws(Exception::class)
            override fun doExecute(context: ClusterSyncContext, payload: ClusterSyncResult, variables: Map<Any, Any>) {
                sendEvent(context)
            }

            override fun createRequest(context: ClusterSyncContext): Selectable {
                return StackEvent(ClusterSyncEvent.FINALIZED_EVENT.stringRepresentation(), context.stack.id)
            }
        }
    }

    @Bean(name = "CLUSTER_SYNC_FAILED_STATE")
    fun clusterSyncFailedAction(): Action<Any, Any> {
        return object : AbstractStackFailureAction<ClusterSyncState, ClusterSyncEvent>() {
            @Throws(Exception::class)
            override fun doExecute(context: StackFailureContext, payload: StackFailureEvent, variables: Map<Any, Any>) {
                LOGGER.warn("Error during executing cluster sync.", payload.exception)
                sendEvent(context)
            }

            override fun createRequest(context: StackFailureContext): Selectable {
                return StackEvent(ClusterSyncEvent.FAIL_HANDLED_EVENT.stringRepresentation(), context.stack.id)
            }
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(ClusterSyncActions::class.java)
    }
}
