package com.sequenceiq.cloudbreak.core.flow2.cluster.reset

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.statemachine.action.Action

import com.sequenceiq.cloudbreak.cloud.event.Selectable
import com.sequenceiq.cloudbreak.core.flow2.cluster.ClusterContext
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction
import com.sequenceiq.cloudbreak.core.flow2.stack.FlowMessageService
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.StartAmbariRequest
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.StartAmbariSuccess
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ClusterResetRequest
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ClusterResetResult
import com.sequenceiq.cloudbreak.repository.StackUpdater
import com.sequenceiq.cloudbreak.service.cluster.ClusterService
import com.sequenceiq.cloudbreak.service.cluster.flow.EmailSenderService

@Configuration
class ClusterResetActions {
    @Inject
    private val flowMessageService: FlowMessageService? = null
    @Inject
    private val clusterService: ClusterService? = null
    @Inject
    private val stackUpdater: StackUpdater? = null
    @Inject
    private val emailSenderService: EmailSenderService? = null

    @Inject
    private val clusterResetService: ClusterResetService? = null

    @Bean(name = "CLUSTER_RESET_STATE")
    fun syncCluster(): Action<Any, Any> {
        return object : AbstractClusterResetAction<StackEvent>(StackEvent::class.java) {
            @Throws(Exception::class)
            override fun doExecute(context: ClusterContext, payload: StackEvent, variables: Map<Any, Any>) {
                clusterResetService!!.resetCluster(context.stack, context.cluster)
                sendEvent(context)
            }

            override fun createRequest(context: ClusterContext): Selectable {
                return ClusterResetRequest(context.stack.id)
            }
        }
    }

    @Bean(name = "CLUSTER_RESET_FINISHED_STATE")
    fun finishResetCluster(): Action<Any, Any> {
        return object : AbstractClusterResetAction<ClusterResetResult>(ClusterResetResult::class.java) {
            @Throws(Exception::class)
            override fun doExecute(context: ClusterContext, payload: ClusterResetResult, variables: Map<Any, Any>) {
                sendEvent(context)
            }

            override fun createRequest(context: ClusterContext): Selectable {
                return StartAmbariRequest(context.stack.id)
            }
        }
    }

    @Bean(name = "CLUSTER_RESET_START_AMBARI_FINISHED_STATE")
    fun finishStartAmbari(): Action<Any, Any> {
        return object : AbstractClusterResetAction<StartAmbariSuccess>(StartAmbariSuccess::class.java) {
            @Throws(Exception::class)
            override fun doExecute(context: ClusterContext, payload: StartAmbariSuccess, variables: Map<Any, Any>) {
                sendEvent(context)
            }

            override fun createRequest(context: ClusterContext): Selectable {
                return StackEvent(ClusterResetEvent.FINALIZED_EVENT.stringRepresentation(), context.stack.id)
            }
        }
    }

    @Bean(name = "CLUSTER_RESET_FAILED_STATE")
    fun clusterResetFailedAction(): Action<Any, Any> {
        return object : AbstractStackFailureAction<ClusterResetState, ClusterResetEvent>() {
            @Throws(Exception::class)
            override fun doExecute(context: StackFailureContext, payload: StackFailureEvent, variables: Map<Any, Any>) {
                clusterResetService!!.handleResetClusterFailure(context.stack, payload.exception.message)
                sendEvent(context)
            }

            override fun createRequest(context: StackFailureContext): Selectable {
                return StackEvent(ClusterResetEvent.FAIL_HANDLED_EVENT.stringRepresentation(), context.stack.id)
            }
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(ClusterResetActions::class.java)
    }
}
