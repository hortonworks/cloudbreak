package com.sequenceiq.cloudbreak.core.flow2.cluster.termination

import javax.inject.Inject

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
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterTerminationRequest
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterTerminationResult

@Configuration
class ClusterTerminationActions {
    @Inject
    private val clusterTerminationFlowService: ClusterTerminationFlowService? = null

    @Bean(name = "CLUSTER_TERMINATING_STATE")
    fun terminatingCluster(): Action<Any, Any> {
        return object : AbstractClusterAction<StackEvent>(StackEvent::class.java) {
            @Throws(Exception::class)
            override fun doExecute(context: ClusterContext, payload: StackEvent, variables: Map<Any, Any>) {
                clusterTerminationFlowService!!.terminateCluster(context)
                sendEvent(context)
            }

            override fun createRequest(context: ClusterContext): Selectable {
                return ClusterTerminationRequest(context.stack.id, context.cluster.id)

            }
        }
    }

    @Bean(name = "CLUSTER_TERMINATION_FINISH_STATE")
    fun clusterTerminationFinished(): Action<Any, Any> {
        return object : AbstractClusterAction<ClusterTerminationResult>(ClusterTerminationResult::class.java) {
            @Throws(Exception::class)
            override fun doExecute(context: ClusterContext, payload: ClusterTerminationResult, variables: Map<Any, Any>) {
                clusterTerminationFlowService!!.finishClusterTermination(context, payload)
                sendEvent(context)
            }

            override fun createRequest(context: ClusterContext): Selectable {
                return StackEvent(ClusterTerminationEvent.FINALIZED_EVENT.stringRepresentation(), context.stack.id)
            }
        }
    }

    @Bean(name = "CLUSTER_TERMINATION_FAILED_STATE")
    fun clusterTerminationFailedAction(): Action<Any, Any> {
        return object : AbstractStackFailureAction<ClusterTerminationState, ClusterTerminationEvent>() {
            @Throws(Exception::class)
            override fun doExecute(context: StackFailureContext, payload: StackFailureEvent, variables: Map<Any, Any>) {
                clusterTerminationFlowService!!.handleClusterTerminationError(payload)
                sendEvent(context)
            }

            override fun createRequest(context: StackFailureContext): Selectable {
                return StackEvent(ClusterTerminationEvent.FAIL_HANDLED_EVENT.stringRepresentation(), context.stack.id)
            }
        }
    }


}
