package com.sequenceiq.cloudbreak.core.flow2.cluster.upscale

import com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.FAIL_HANDLED_EVENT
import com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.FINALIZED_EVENT
import java.util.Optional

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.statemachine.StateContext
import org.springframework.statemachine.action.Action

import com.sequenceiq.cloudbreak.cloud.event.Payload
import com.sequenceiq.cloudbreak.cloud.event.Selectable
import com.sequenceiq.cloudbreak.core.flow2.AbstractAction
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterScaleTriggerEvent
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.logger.MDCBuilder
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.UpscaleClusterRequest
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.UpscaleClusterResult
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.UpscaleAmbariRequest
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.UpscaleAmbariResult
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.UpscalePostRecipesRequest
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.UpscalePostRecipesResult
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.UpscalePreRecipesRequest
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.UpscalePreRecipesResult
import com.sequenceiq.cloudbreak.service.stack.StackService

@Configuration
class ClusterUpscaleActions {
    @Inject
    private val clusterUpscaleFlowService: ClusterUpscaleFlowService? = null

    @Bean(name = "UPSCALING_AMBARI_STATE")
    fun upscalingAmbariAction(): Action<Any, Any> {
        return object : AbstractClusterUpscaleAction<ClusterScaleTriggerEvent>(ClusterScaleTriggerEvent::class.java) {
            override fun prepareExecution(payload: ClusterScaleTriggerEvent, variables: MutableMap<Any, Any>) {
                variables.put(ClusterUpscaleActions.AbstractClusterUpscaleAction.HOSTGROUPNAME, payload.hostGroupName)
                variables.put(ClusterUpscaleActions.AbstractClusterUpscaleAction.ADJUSTMENT, payload.adjustment)
            }

            @Throws(Exception::class)
            override fun doExecute(context: ClusterUpscaleContext, payload: ClusterScaleTriggerEvent, variables: Map<Any, Any>) {
                clusterUpscaleFlowService!!.upscalingAmbari(context.stack)
                sendEvent(context)
            }

            override fun createRequest(context: ClusterUpscaleContext): Selectable {
                return UpscaleAmbariRequest(context.stack.id, context.hostGroupName, context.adjustment)
            }

        }
    }

    @Bean(name = "EXECUTING_PRERECIPES_STATE")
    fun executingPrerecipesAction(): Action<Any, Any> {
        return object : AbstractClusterUpscaleAction<UpscaleAmbariResult>(UpscaleAmbariResult::class.java) {
            @Throws(Exception::class)
            override fun doExecute(context: ClusterUpscaleContext, payload: UpscaleAmbariResult, variables: Map<Any, Any>) {
                sendEvent(context)
            }

            override fun createRequest(context: ClusterUpscaleContext): Selectable {
                return UpscalePreRecipesRequest(context.stack.id, context.hostGroupName)
            }
        }
    }

    @Bean(name = "UPSCALING_CLUSTER_STATE")
    fun installServicesAction(): Action<Any, Any> {
        return object : AbstractClusterUpscaleAction<UpscalePreRecipesResult>(UpscalePreRecipesResult::class.java) {

            @Throws(Exception::class)
            override fun doExecute(context: ClusterUpscaleContext, payload: UpscalePreRecipesResult, variables: Map<Any, Any>) {
                sendEvent(context)
            }

            override fun createRequest(context: ClusterUpscaleContext): Selectable {
                return UpscaleClusterRequest(context.stack.id, context.hostGroupName)
            }
        }
    }

    @Bean(name = "EXECUTING_POSTRECIPES_STATE")
    fun executePostRecipesAction(): Action<Any, Any> {
        return object : AbstractClusterUpscaleAction<UpscaleClusterResult>(UpscaleClusterResult::class.java) {

            @Throws(Exception::class)
            override fun doExecute(context: ClusterUpscaleContext, payload: UpscaleClusterResult, variables: Map<Any, Any>) {
                sendEvent(context)
            }

            override fun createRequest(context: ClusterUpscaleContext): Selectable {
                return UpscalePostRecipesRequest(context.stack.id, context.hostGroupName)
            }
        }
    }

    @Bean(name = "FINALIZE_UPSCALE_STATE")
    fun upscaleFinishedAction(): Action<Any, Any> {
        return object : AbstractClusterUpscaleAction<UpscalePostRecipesResult>(UpscalePostRecipesResult::class.java) {

            @Throws(Exception::class)
            override fun doExecute(context: ClusterUpscaleContext, payload: UpscalePostRecipesResult, variables: Map<Any, Any>) {
                clusterUpscaleFlowService!!.clusterUpscaleFinished(context.stack, payload.hostGroupName)
                sendEvent(context.flowId, FINALIZED_EVENT.stringRepresentation(), payload)
            }

            override fun createRequest(context: ClusterUpscaleContext): Selectable? {
                return null
            }
        }
    }

    @Bean(name = "CLUSTER_UPSCALE_FAILED_STATE")
    fun clusterUpscaleFailedAction(): Action<Any, Any> {
        return object : AbstractStackFailureAction<ClusterUpscaleState, ClusterUpscaleEvent>() {

            @Throws(Exception::class)
            override fun doExecute(context: StackFailureContext, payload: StackFailureEvent, variables: Map<Any, Any>) {
                clusterUpscaleFlowService!!.clusterUpscaleFailed(context.stack, payload.exception)
                sendEvent(context.flowId, FAIL_HANDLED_EVENT.stringRepresentation(), payload)
            }
        }
    }

    private abstract inner class AbstractClusterUpscaleAction<P : Payload> protected constructor(payloadClass: Class<P>) : AbstractAction<ClusterUpscaleState, ClusterUpscaleEvent, ClusterUpscaleContext, P>(payloadClass) {

        @Inject
        private val stackService: StackService? = null

        override fun getFailurePayload(payload: P, flowContext: Optional<ClusterUpscaleContext>, ex: Exception): Any {
            return StackFailureEvent(payload.stackId, ex)
        }

        override fun createFlowContext(flowId: String, stateContext: StateContext<ClusterUpscaleState, ClusterUpscaleEvent>, payload: P): ClusterUpscaleContext {
            val variables = stateContext.extendedState.variables
            val stack = stackService!!.getById(payload.stackId)
            MDCBuilder.buildMdcContext(stack.cluster)
            return ClusterUpscaleContext(flowId, stack, getHostgroupName(variables), getAdjustment(variables))
        }

        private fun getHostgroupName(variables: Map<Any, Any>): String {
            return variables[HOSTGROUPNAME] as String
        }

        private fun getAdjustment(variables: Map<Any, Any>): Int? {
            return variables[ADJUSTMENT] as Int
        }

        companion object {
            protected val HOSTGROUPNAME = "HOSTGROUPNAME"
            protected val ADJUSTMENT = "ADJUSTMENT"
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(ClusterUpscaleActions::class.java)
    }
}
