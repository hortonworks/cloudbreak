package com.sequenceiq.cloudbreak.core.flow2.stack.upscale

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.statemachine.action.Action

import com.sequenceiq.cloudbreak.api.model.Status
import com.sequenceiq.cloudbreak.cloud.event.Selectable
import com.sequenceiq.cloudbreak.cloud.event.instance.CollectMetadataRequest
import com.sequenceiq.cloudbreak.cloud.event.instance.CollectMetadataResult
import com.sequenceiq.cloudbreak.cloud.event.resource.UpscaleStackRequest
import com.sequenceiq.cloudbreak.cloud.event.resource.UpscaleStackResult
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance
import com.sequenceiq.cloudbreak.cloud.model.CloudResource
import com.sequenceiq.cloudbreak.cloud.model.CloudStack
import com.sequenceiq.cloudbreak.converter.spi.CloudResourceToResourceConverter
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter
import com.sequenceiq.cloudbreak.converter.spi.StackToCloudStackConverter
import com.sequenceiq.cloudbreak.core.flow2.event.StackScaleTriggerEvent
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction
import com.sequenceiq.cloudbreak.core.flow2.stack.FlowMessageService
import com.sequenceiq.cloudbreak.core.flow2.stack.Msg
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext
import com.sequenceiq.cloudbreak.core.flow2.stack.downscale.StackScalingFlowContext
import com.sequenceiq.cloudbreak.domain.InstanceGroup
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent
import com.sequenceiq.cloudbreak.reactor.api.event.resource.BootstrapNewNodesRequest
import com.sequenceiq.cloudbreak.reactor.api.event.resource.BootstrapNewNodesResult
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ExtendHostMetadataRequest
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ExtendHostMetadataResult
import com.sequenceiq.cloudbreak.service.cluster.ClusterService
import com.sequenceiq.cloudbreak.service.stack.InstanceMetadataService

@Configuration
class StackUpscaleActions {
    @Inject
    private val cloudResourceConverter: ResourceToCloudResourceConverter? = null
    @Inject
    private val resourceConverter: CloudResourceToResourceConverter? = null
    @Inject
    private val instanceMetadataService: InstanceMetadataService? = null
    @Inject
    private val stackUpscaleService: StackUpscaleService? = null
    @Inject
    private val clusterService: ClusterService? = null
    @Inject
    private val cloudStackConverter: StackToCloudStackConverter? = null
    @Inject
    private val flowMessageService: FlowMessageService? = null

    @Bean(name = "ADD_INSTANCES_STATE")
    fun addInstances(): Action<Any, Any> {
        return object : AbstractStackUpscaleAction<StackScaleTriggerEvent>(StackScaleTriggerEvent::class.java) {
            override fun prepareExecution(payload: StackScaleTriggerEvent, variables: MutableMap<Any, Any>) {
                variables.put(AbstractStackUpscaleAction.INSTANCEGROUPNAME, payload.instanceGroup)
                variables.put(AbstractStackUpscaleAction.ADJUSTMENT, payload.adjustment)
            }

            @Throws(Exception::class)
            override fun doExecute(context: StackScalingFlowContext, payload: StackScaleTriggerEvent, variables: Map<Any, Any>) {
                stackUpscaleService!!.startAddInstances(context.stack, payload.adjustment)
                sendEvent(context)
            }

            override fun createRequest(context: StackScalingFlowContext): Selectable {
                LOGGER.debug("Assembling upscale stack event for stack: {}", context.stack)
                val group = context.stack.getInstanceGroupByInstanceGroupName(context.instanceGroupName)
                group.nodeCount = group.nodeCount!! + context.adjustment!!
                val cloudStack = cloudStackConverter!!.convert(context.stack)
                instanceMetadataService!!.saveInstanceRequests(context.stack, cloudStack.groups)
                val resources = cloudResourceConverter!!.convert(context.stack.resources)
                return UpscaleStackRequest<UpscaleStackResult>(context.cloudContext, context.cloudCredential, cloudStack, resources)
            }
        }
    }

    @Bean(name = "ADD_INSTANCES_FINISHED_STATE")
    fun finishAddInstances(): Action<Any, Any> {
        return object : AbstractStackUpscaleAction<UpscaleStackResult>(UpscaleStackResult::class.java) {
            @Throws(Exception::class)
            override fun doExecute(context: StackScalingFlowContext, payload: UpscaleStackResult, variables: Map<Any, Any>) {
                stackUpscaleService!!.finishAddInstances(context, payload)
                sendEvent(context)
            }

            override fun createRequest(context: StackScalingFlowContext): Selectable {
                return StackEvent(StackUpscaleEvent.EXTEND_METADATA_EVENT.stringRepresentation(), context.stack.id)
            }
        }
    }

    @Bean(name = "EXTEND_METADATA_STATE")
    fun extendMetadata(): Action<Any, Any> {
        return object : AbstractStackUpscaleAction<StackEvent>(StackEvent::class.java) {
            @Throws(Exception::class)
            override fun doExecute(context: StackScalingFlowContext, payload: StackEvent, variables: Map<Any, Any>) {
                clusterService!!.updateClusterStatusByStackId(context.stack.id, Status.UPDATE_IN_PROGRESS)
                sendEvent(context)
            }

            override fun createRequest(context: StackScalingFlowContext): Selectable {
                val cloudResources = cloudResourceConverter!!.convert(context.stack.resources)
                val cloudInstances = stackUpscaleService!!.getNewInstances(context.stack)
                return CollectMetadataRequest(context.cloudContext, context.cloudCredential, cloudResources, cloudInstances)
            }
        }
    }

    @Bean(name = "EXTEND_METADATA_FINISHED_STATE")
    fun finishExtendMetadata(): Action<Any, Any> {
        return object : AbstractStackUpscaleAction<CollectMetadataResult>(CollectMetadataResult::class.java) {
            @Throws(Exception::class)
            override fun doExecute(context: StackScalingFlowContext, payload: CollectMetadataResult, variables: Map<Any, Any>) {
                val upscaleCandidateAddresses = stackUpscaleService!!.finishExtendMetadata(context.stack, context.instanceGroupName, payload)
                val bootstrapPayload = BootstrapNewNodesEvent(context.stack.id, upscaleCandidateAddresses)
                sendEvent(context.flowId, StackUpscaleEvent.BOOTSTRAP_NEW_NODES_EVENT.stringRepresentation(), bootstrapPayload)
            }
        }
    }

    @Bean(name = "BOOTSTRAP_NEW_NODES_STATE")
    fun bootstrapNewNodes(): Action<Any, Any> {
        return object : AbstractStackUpscaleAction<BootstrapNewNodesEvent>(BootstrapNewNodesEvent::class.java) {
            @Throws(Exception::class)
            override fun doExecute(context: StackScalingFlowContext, payload: BootstrapNewNodesEvent, variables: Map<Any, Any>) {
                flowMessageService!!.fireEventAndLog(context.stack.id, Msg.STACK_BOOTSTRAP_NEW_NODES, Status.UPDATE_IN_PROGRESS.name)
                val request = BootstrapNewNodesRequest(context.stack.id, payload.upscaleCandidateAddresses)
                sendEvent(context.flowId, request)
            }
        }
    }

    @Bean(name = "EXTEND_HOST_METADATA_STATE")
    fun extendHostMetadata(): Action<Any, Any> {
        return object : AbstractStackUpscaleAction<BootstrapNewNodesResult>(BootstrapNewNodesResult::class.java) {
            @Throws(Exception::class)
            override fun doExecute(context: StackScalingFlowContext, payload: BootstrapNewNodesResult, variables: Map<Any, Any>) {
                val request = ExtendHostMetadataRequest(context.stack.id,
                        payload.request.upscaleCandidateAddresses)
                sendEvent(context.flowId, request)
            }
        }
    }

    @Bean(name = "EXTEND_HOST_METADATA_FINISHED_STATE")
    fun finishExtendHostMetadata(): Action<Any, Any> {
        return object : AbstractStackUpscaleAction<ExtendHostMetadataResult>(ExtendHostMetadataResult::class.java) {
            @Throws(Exception::class)
            override fun doExecute(context: StackScalingFlowContext, payload: ExtendHostMetadataResult, variables: Map<Any, Any>) {
                stackUpscaleService!!.finishExtendHostMetadata(context.stack)
                sendEvent(context)
            }

            override fun createRequest(context: StackScalingFlowContext): Selectable {
                return StackEvent(StackUpscaleEvent.UPSCALE_FINALIZED_EVENT.stringRepresentation(), context.stack.id)
            }
        }
    }

    @Bean(name = "UPSCALE_FAILED_STATE")
    fun stackStartFailedAction(): Action<Any, Any> {
        return object : AbstractStackFailureAction<StackUpscaleState, StackUpscaleEvent>() {
            @Throws(Exception::class)
            override fun doExecute(context: StackFailureContext, payload: StackFailureEvent, variables: Map<Any, Any>) {
                stackUpscaleService!!.handleStackUpscaleFailure(context.stack, payload)
                sendEvent(context)
            }

            override fun createRequest(context: StackFailureContext): Selectable {
                return StackEvent(StackUpscaleEvent.UPSCALE_FAIL_HANDLED_EVENT.stringRepresentation(), context.stack.id)
            }
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(StackUpscaleActions::class.java)
    }
}
