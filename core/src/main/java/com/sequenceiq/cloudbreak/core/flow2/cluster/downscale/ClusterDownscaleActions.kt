package com.sequenceiq.cloudbreak.core.flow2.cluster.downscale

import com.sequenceiq.cloudbreak.api.model.Status.AVAILABLE
import com.sequenceiq.cloudbreak.api.model.Status.UPDATE_FAILED
import com.sequenceiq.cloudbreak.api.model.Status.UPDATE_IN_PROGRESS
import com.sequenceiq.cloudbreak.core.flow2.cluster.downscale.ClusterDownscaleEvent.FAIL_HANDLED_EVENT
import com.sequenceiq.cloudbreak.core.flow2.cluster.downscale.ClusterDownscaleEvent.FINALIZED_EVENT
import java.util.Optional

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.statemachine.StateContext
import org.springframework.statemachine.action.Action

import com.sequenceiq.cloudbreak.cloud.event.Selectable
import com.sequenceiq.cloudbreak.core.flow2.AbstractAction
import com.sequenceiq.cloudbreak.core.flow2.PayloadConverter
import com.sequenceiq.cloudbreak.core.flow2.cluster.ClusterScaleContext
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterScaleTriggerEvent
import com.sequenceiq.cloudbreak.core.flow2.stack.FlowMessageService
import com.sequenceiq.cloudbreak.core.flow2.stack.Msg
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.logger.MDCBuilder
import com.sequenceiq.cloudbreak.reactor.api.event.ClusterScalePayload
import com.sequenceiq.cloudbreak.reactor.api.event.HostGroupPayload
import com.sequenceiq.cloudbreak.reactor.api.event.ScalingAdjustmentPayload
import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleResult
import com.sequenceiq.cloudbreak.reactor.api.event.resource.DecommissionRequest
import com.sequenceiq.cloudbreak.reactor.api.event.resource.DecommissionResult
import com.sequenceiq.cloudbreak.reactor.api.event.resource.UpdateInstanceMetadataRequest
import com.sequenceiq.cloudbreak.reactor.api.event.resource.UpdateInstanceMetadataResult
import com.sequenceiq.cloudbreak.repository.StackUpdater
import com.sequenceiq.cloudbreak.service.cluster.ClusterService
import com.sequenceiq.cloudbreak.service.stack.StackService

@Configuration
class ClusterDownscaleActions {

    @Inject
    private val stackService: StackService? = null

    @Inject
    private val flowMessageService: FlowMessageService? = null

    @Bean(name = "DECOMMISSION_STATE")
    fun decommissionAction(): Action<Any, Any> {
        return object : AbstractClusterDownscaleAction<ClusterDecommissionContext, ClusterScaleTriggerEvent>(ClusterScaleTriggerEvent::class.java) {

            override fun createFlowContext(flowId: String, stateContext: StateContext<ClusterDownscaleState, ClusterDownscaleEvent>,
                                           payload: ClusterScaleTriggerEvent): ClusterDecommissionContext {
                val stack = stackService!!.getById(payload.stackId)
                MDCBuilder.buildMdcContext(stack)
                return ClusterDecommissionContext(flowId, stack, payload.hostGroupName, payload.adjustment)
            }

            @Throws(Exception::class)
            override fun doExecute(context: ClusterDecommissionContext, payload: ClusterScaleTriggerEvent, variables: MutableMap<Any, Any>) {
                variables.put(SCALING_ADJUSTMENT, context.scalingAdjustment)
                flowMessageService!!.fireEventAndLog(context.stack.id, Msg.AMBARI_CLUSTER_SCALING_DOWN, UPDATE_IN_PROGRESS.name)
                sendEvent(context)
            }

            override fun createRequest(context: ClusterDecommissionContext): Selectable {
                return DecommissionRequest(context.stack.id, context.hostGroupName, context.scalingAdjustment)
            }
        }
    }

    @Bean(name = "UPDATE_INSTANCE_METADATA_STATE")
    fun updateInstanceMetadataAction(): Action<Any, Any> {
        return object : AbstractClusterDownscaleAction<ClusterUpdateMetadataContext, DecommissionResult>(DecommissionResult::class.java) {

            override fun createFlowContext(flowId: String, stateContext: StateContext<ClusterDownscaleState, ClusterDownscaleEvent>,
                                           payload: DecommissionResult): ClusterUpdateMetadataContext {
                val stack = stackService!!.getById(payload.stackId)
                MDCBuilder.buildMdcContext(stack)
                return ClusterUpdateMetadataContext(flowId, stack, payload.request.hostGroupName, payload.hostNames)
            }

            @Throws(Exception::class)
            override fun doExecute(context: ClusterUpdateMetadataContext, payload: DecommissionResult, variables: Map<Any, Any>) {
                sendEvent(context)
            }

            override fun createRequest(context: ClusterUpdateMetadataContext): Selectable {
                return UpdateInstanceMetadataRequest(context.stack.id, context.hostGroupName, context.hostNames)
            }
        }
    }

    @Bean(name = "FINALIZE_DOWNSCALE_STATE")
    fun finalizeDownscaleAction(): Action<Any, Any> {
        return object : AbstractClusterDownscaleAction<ClusterScaleContext, UpdateInstanceMetadataResult>(UpdateInstanceMetadataResult::class.java) {

            @Throws(Exception::class)
            override fun doExecute(context: ClusterScaleContext, payload: UpdateInstanceMetadataResult, variables: Map<Any, Any>) {
                flowMessageService!!.fireEventAndLog(context.stack.id, Msg.AMBARI_CLUSTER_SCALED_DOWN, AVAILABLE.name)
                val nextPayload = ClusterScalePayload(context.stack.id, context.hostGroupName, variables[SCALING_ADJUSTMENT] as Int)
                sendEvent(context.flowId, FINALIZED_EVENT.stringRepresentation(), nextPayload)
            }

            override fun createRequest(context: ClusterScaleContext): Selectable? {
                return null
            }
        }
    }

    @Bean(name = "CLUSTER_DOWNSCALE_FAILED_STATE")
    fun clusterDownscalescaleFailedAction(): Action<Any, Any> {
        return object : AbstractClusterDownscaleAction<ClusterScaleContext, DownscaleClusterFailurePayload>(DownscaleClusterFailurePayload::class.java) {
            @Inject
            private val stackUpdater: StackUpdater? = null
            @Inject
            private val clusterService: ClusterService? = null
            @Inject
            private val flowMessageService: FlowMessageService? = null

            @Throws(Exception::class)
            override fun doExecute(context: ClusterScaleContext, payload: DownscaleClusterFailurePayload, variables: Map<Any, Any>) {
                getFlow(context.flowId).setFlowFailed()
                val stack = context.stack
                val message = payload.errorDetails.message
                LOGGER.error("Error during Cluster downscale flow: " + message, payload.errorDetails)
                clusterService!!.updateClusterStatusByStackId(stack.id, UPDATE_FAILED, message)
                stackUpdater!!.updateStackStatus(stack.id, AVAILABLE, "Node(s) could not be removed from the cluster: " + message)
                flowMessageService!!.fireEventAndLog(stack.id, Msg.AMBARI_CLUSTER_SCALING_FAILED, UPDATE_FAILED.name, "removed from", message)
                sendEvent(context.flowId, FAIL_HANDLED_EVENT.stringRepresentation(), payload)
            }

            override fun initPayloadConverterMap(payloadConverters: MutableList<PayloadConverter<DownscaleClusterFailurePayload>>) {
                payloadConverters.add(object : PayloadConverter<DownscaleClusterFailurePayload> {
                    override fun canConvert(sourceClass: Class<Any>): Boolean {
                        return AbstractClusterScaleResult<AbstractClusterScaleRequest>::class.java!!.isAssignableFrom(sourceClass)
                    }

                    override fun convert(payload: Any): DownscaleClusterFailurePayload {
                        val result = payload as AbstractClusterScaleResult<AbstractClusterScaleRequest>
                        return DownscaleClusterFailurePayload(result.stackId, result.hostGroupName, result.errorDetails)
                    }
                })
            }

            override fun createRequest(context: ClusterScaleContext): Selectable? {
                return null
            }
        }
    }

    private abstract inner class AbstractClusterDownscaleAction<C : ClusterScaleContext, P : HostGroupPayload> internal constructor(payloadClass: Class<P>) : AbstractAction<ClusterDownscaleState, ClusterDownscaleEvent, C, P>(payloadClass) {

        override fun createFlowContext(flowId: String, stateContext: StateContext<ClusterDownscaleState, ClusterDownscaleEvent>, payload: P): C {
            val stack = stackService!!.getById(payload.stackId)
            MDCBuilder.buildMdcContext(stack)
            return ClusterScaleContext(flowId, stack, payload.hostGroupName) as C
        }

        override fun getFailurePayload(payload: P, flowContext: Optional<C>, ex: Exception): Any {
            return DownscaleClusterFailurePayload(payload.stackId, payload.hostGroupName, ex)
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(ClusterDownscaleActions::class.java)
        private val SCALING_ADJUSTMENT = "SCALING_ADJUSTMENT"
    }
}
