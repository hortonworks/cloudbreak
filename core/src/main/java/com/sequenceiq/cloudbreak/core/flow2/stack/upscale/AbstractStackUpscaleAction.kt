package com.sequenceiq.cloudbreak.core.flow2.stack.upscale

import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone
import com.sequenceiq.cloudbreak.cloud.model.Location.location
import com.sequenceiq.cloudbreak.cloud.model.Region.region

import java.util.Collections
import java.util.Optional

import javax.inject.Inject

import org.springframework.statemachine.StateContext

import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.event.Payload
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential
import com.sequenceiq.cloudbreak.cloud.model.CloudStack
import com.sequenceiq.cloudbreak.cloud.model.Location
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter
import com.sequenceiq.cloudbreak.converter.spi.StackToCloudStackConverter
import com.sequenceiq.cloudbreak.core.flow2.AbstractAction
import com.sequenceiq.cloudbreak.core.flow2.stack.downscale.StackScalingFlowContext
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.logger.MDCBuilder
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent
import com.sequenceiq.cloudbreak.service.stack.StackService
import com.sequenceiq.cloudbreak.service.stack.flow.StackScalingService

internal abstract class AbstractStackUpscaleAction<P : Payload>(payloadClass: Class<P>) : AbstractAction<StackUpscaleState, StackUpscaleEvent, StackScalingFlowContext, P>(payloadClass) {

    @Inject
    private val stackService: StackService? = null
    @Inject
    private val cloudStackConverter: StackToCloudStackConverter? = null
    @Inject
    private val credentialConverter: CredentialToCloudCredentialConverter? = null
    @Inject
    private val cloudResourceConverter: ResourceToCloudResourceConverter? = null
    @Inject
    private val stackScalingService: StackScalingService? = null

    override fun createFlowContext(flowId: String, stateContext: StateContext<StackUpscaleState, StackUpscaleEvent>, payload: P): StackScalingFlowContext {
        val variables = stateContext.extendedState.variables
        val stack = stackService!!.getById(payload.stackId)
        MDCBuilder.buildMdcContext(stack)
        val location = Companion.location(Companion.region(stack.region), Companion.availabilityZone(stack.availabilityZone))
        val cloudContext = CloudContext(stack.id, stack.name, stack.cloudPlatform(), stack.owner, stack.platformVariant,
                location)
        val cloudCredential = credentialConverter!!.convert(stack.credential)
        val cloudStack = cloudStackConverter!!.convert(stack)
        return StackScalingFlowContext(flowId, stack, cloudContext, cloudCredential, cloudStack, getInstanceGroupName(variables), emptySet<String>(),
                getAdjustment(variables))
    }

    override fun getFailurePayload(payload: P, flowContext: Optional<StackScalingFlowContext>, ex: Exception): Any {
        return StackFailureEvent(payload.stackId, ex)
    }

    private fun getInstanceGroupName(variables: Map<Any, Any>): String {
        return variables[INSTANCEGROUPNAME] as String
    }

    private fun getAdjustment(variables: Map<Any, Any>): Int? {
        return variables[ADJUSTMENT] as Int
    }

    companion object {
        val INSTANCEGROUPNAME = "INSTANCEGROUPNAME"
        val ADJUSTMENT = "ADJUSTMENT"
    }
}
