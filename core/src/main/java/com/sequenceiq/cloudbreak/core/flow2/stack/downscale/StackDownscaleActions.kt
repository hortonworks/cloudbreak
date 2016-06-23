package com.sequenceiq.cloudbreak.core.flow2.stack.downscale

import java.util.ArrayList

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.statemachine.action.Action

import com.sequenceiq.cloudbreak.cloud.event.Selectable
import com.sequenceiq.cloudbreak.cloud.event.resource.DownscaleStackRequest
import com.sequenceiq.cloudbreak.cloud.event.resource.DownscaleStackResult
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance
import com.sequenceiq.cloudbreak.cloud.model.CloudResource
import com.sequenceiq.cloudbreak.converter.spi.InstanceMetaDataToCloudInstanceConverter
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter
import com.sequenceiq.cloudbreak.core.flow2.event.StackScaleTriggerEvent
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext
import com.sequenceiq.cloudbreak.domain.InstanceGroup
import com.sequenceiq.cloudbreak.domain.InstanceMetaData
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent

@Configuration
class StackDownscaleActions {

    @Inject
    private val metadataConverter: InstanceMetaDataToCloudInstanceConverter? = null
    @Inject
    private val cloudResourceConverter: ResourceToCloudResourceConverter? = null
    @Inject
    private val stackDownscaleService: StackDownscaleService? = null

    @Bean(name = "DOWNSCALE_STATE")
    fun stackDownscaleAction(): Action<Any, Any> {
        return object : AbstractStackDownscaleAction<StackScaleTriggerEvent>(StackScaleTriggerEvent::class.java) {
            @Throws(Exception::class)
            override fun doExecute(context: StackScalingFlowContext, payload: StackScaleTriggerEvent, variables: Map<Any, Any>) {
                stackDownscaleService!!.startStackDownscale(context, payload.adjustment)
                sendEvent(context)
            }

            override fun createRequest(context: StackScalingFlowContext): Selectable {
                val stack = context.stack
                LOGGER.debug("Assembling downscale stack event for stack: {}", stack)
                val resources = cloudResourceConverter!!.convert(stack.resources)
                val instances = ArrayList<CloudInstance>()
                val group = stack.getInstanceGroupByInstanceGroupName(context.instanceGroupName)
                for (metaData in group.allInstanceMetaData) {
                    if (context.instanceIds.contains(metaData.instanceId)) {
                        val cloudInstance = metadataConverter!!.convert(metaData)
                        instances.add(cloudInstance)
                    }
                }
                return DownscaleStackRequest<Any>(context.cloudContext, context.cloudCredential, context.cloudStack, resources, instances)
            }
        }
    }

    @Bean(name = "DOWNSCALE_FINISHED_STATE")
    fun stackDownscaleFinishedAction(): Action<Any, Any> {
        return object : AbstractStackDownscaleAction<DownscaleStackResult>(DownscaleStackResult::class.java) {
            @Throws(Exception::class)
            override fun doExecute(context: StackScalingFlowContext, payload: DownscaleStackResult, variables: Map<Any, Any>) {
                stackDownscaleService!!.finishStackDownscale(context, getInstanceGroupName(variables), getInstanceIds(variables))
                sendEvent(context)
            }

            override fun createRequest(context: StackScalingFlowContext): Selectable {
                return StackEvent(StackDownscaleEvent.DOWNSCALE_FINALIZED_EVENT.stringRepresentation(), context.stack.id)
            }
        }
    }

    @Bean(name = "DOWNSCALE_FAILED_STATE")
    fun stackDownscaleFailedAction(): Action<Any, Any> {
        return object : AbstractStackFailureAction<StackDownscaleState, StackDownscaleEvent>() {
            @Throws(Exception::class)
            override fun doExecute(context: StackFailureContext, payload: StackFailureEvent, variables: Map<Any, Any>) {
                stackDownscaleService!!.handleStackDownscaleError(payload.exception)
                sendEvent(context)
            }

            override fun createRequest(context: StackFailureContext): Selectable {
                return StackEvent(StackDownscaleEvent.DOWNSCALE_FAIL_HANDLED_EVENT.stringRepresentation(), context.stack.id)
            }
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(StackDownscaleActions::class.java)
    }
}
