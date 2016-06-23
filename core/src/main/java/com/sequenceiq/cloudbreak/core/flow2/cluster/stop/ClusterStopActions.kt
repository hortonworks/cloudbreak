package com.sequenceiq.cloudbreak.core.flow2.cluster.stop


import javax.inject.Inject

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.statemachine.action.Action

import com.sequenceiq.cloudbreak.api.model.Status
import com.sequenceiq.cloudbreak.cloud.event.Selectable
import com.sequenceiq.cloudbreak.core.flow2.cluster.AbstractClusterAction
import com.sequenceiq.cloudbreak.core.flow2.cluster.ClusterContext
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterStopRequest
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterStopResult

@Configuration
class ClusterStopActions {
    @Inject
    private val clusterStopService: ClusterStopService? = null

    @Bean(name = "CLUSTER_STOPPING_STATE")
    fun stoppingCluster(): Action<Any, Any> {
        return object : AbstractClusterAction<StackEvent>(StackEvent::class.java) {
            @Throws(Exception::class)
            override fun doExecute(context: ClusterContext, payload: StackEvent, variables: MutableMap<Any, Any>) {
                val stack = context.stack
                variables.put(STACK_STATUS, stack.status)
                clusterStopService!!.stoppingCluster(stack)
                sendEvent(context)
            }

            override fun createRequest(context: ClusterContext): Selectable {
                return ClusterStopRequest(context.stack.id)
            }
        }
    }

    @Bean(name = "CLUSTER_STOP_FINISHED_STATE")
    fun clusterStopFinished(): Action<Any, Any> {
        return object : AbstractClusterAction<ClusterStopResult>(ClusterStopResult::class.java) {
            @Throws(Exception::class)
            override fun doExecute(context: ClusterContext, payload: ClusterStopResult, variables: Map<Any, Any>) {
                val statusBeforeAmbariStop = variables[STACK_STATUS] as Status
                clusterStopService!!.clusterStopFinished(context.stack, statusBeforeAmbariStop)
                sendEvent(context)
            }

            override fun createRequest(context: ClusterContext): Selectable {
                return StackEvent(ClusterStopEvent.FINALIZED_EVENT.stringRepresentation(), context.stack.id)
            }
        }
    }

    @Bean(name = "CLUSTER_STOP_FAILED_STATE")
    fun clusterStopFailedAction(): Action<Any, Any> {
        return object : AbstractStackFailureAction<ClusterStopState, ClusterStopEvent>() {
            @Throws(Exception::class)
            override fun doExecute(context: StackFailureContext, payload: StackFailureEvent, variables: Map<Any, Any>) {
                clusterStopService!!.handleClusterStopFailure(context.stack, payload.exception.message)
                sendEvent(context)
            }

            override fun createRequest(context: StackFailureContext): Selectable {
                return StackEvent(ClusterStopEvent.FAIL_HANDLED_EVENT.stringRepresentation(), context.stack.id)
            }
        }
    }

    companion object {
        private val STACK_STATUS = "STACK_STATUS"
    }
}
