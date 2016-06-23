package com.sequenceiq.cloudbreak.core.flow2.cluster.start

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.statemachine.action.Action

import com.sequenceiq.cloudbreak.cloud.event.Selectable
import com.sequenceiq.cloudbreak.core.flow2.cluster.AbstractClusterAction
import com.sequenceiq.cloudbreak.core.flow2.cluster.ClusterContext
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterStartRequest
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterStartResult
import com.sequenceiq.cloudbreak.service.cluster.ClusterService

@Configuration
class ClusterStartActions {
    @Inject
    private val clusterService: ClusterService? = null
    @Inject
    private val clusterStartService: ClusterStartService? = null

    @Bean(name = "CLUSTER_STARTING_STATE")
    fun startingCluster(): Action<Any, Any> {
        return object : AbstractClusterAction<StackEvent>(StackEvent::class.java) {
            @Throws(Exception::class)
            override fun doExecute(context: ClusterContext, payload: StackEvent, variables: Map<Any, Any>) {
                clusterStartService!!.startingCluster(context.stack, context.cluster)
                sendEvent(context)
            }

            override fun createRequest(context: ClusterContext): Selectable {
                return ClusterStartRequest(context.stack.id)
            }
        }
    }

    @Bean(name = "CLUSTER_START_FINISHED_STATE")
    fun clusterStartFinished(): Action<Any, Any> {
        return object : AbstractClusterAction<ClusterStartResult>(ClusterStartResult::class.java) {
            @Throws(Exception::class)
            override fun doExecute(context: ClusterContext, payload: ClusterStartResult, variables: Map<Any, Any>) {
                clusterStartService!!.clusterStartFinished(context.stack)
                sendEvent(context)
            }

            override fun createRequest(context: ClusterContext): Selectable {
                return StackEvent(ClusterStartEvent.FINALIZED_EVENT.stringRepresentation(), context.stack.id)
            }
        }
    }

    @Bean(name = "CLUSTER_START_FAILED_STATE")
    fun clusterStartFailedAction(): Action<Any, Any> {
        return object : AbstractStackFailureAction<ClusterStartState, ClusterStartEvent>() {
            @Throws(Exception::class)
            override fun doExecute(context: StackFailureContext, payload: StackFailureEvent, variables: Map<Any, Any>) {
                clusterStartService!!.handleClusterStartFailure(context.stack, payload.exception.message)
                sendEvent(context)
            }

            override fun createRequest(context: StackFailureContext): Selectable {
                return StackEvent(ClusterStartEvent.FAIL_HANDLED_EVENT.stringRepresentation(), context.stack.id)
            }
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(ClusterStartActions::class.java)
    }
}
