package com.sequenceiq.cloudbreak.core.flow2.stack.downscale

import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone
import com.sequenceiq.cloudbreak.cloud.model.Location.location
import com.sequenceiq.cloudbreak.cloud.model.Region.region

import java.util.HashSet
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
import com.sequenceiq.cloudbreak.core.flow2.event.StackScaleTriggerEvent
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.logger.MDCBuilder
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent
import com.sequenceiq.cloudbreak.service.stack.StackService
import com.sequenceiq.cloudbreak.service.stack.flow.StackScalingService

abstract class AbstractStackDownscaleAction<P : Payload> protected constructor(payloadClass: Class<P>) : AbstractAction<StackDownscaleState, StackDownscaleEvent, StackScalingFlowContext, P>(payloadClass) {

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

    override fun createFlowContext(flowId: String, stateContext: StateContext<StackDownscaleState, StackDownscaleEvent>, payload: P): StackScalingFlowContext {
        val variables = stateContext.extendedState.variables
        val stack = stackService!!.getById(payload.stackId)
        MDCBuilder.buildMdcContext(stack)
        val location = Companion.location(Companion.region(stack.region), Companion.availabilityZone(stack.availabilityZone))
        val cloudContext = CloudContext(stack.id, stack.name, stack.cloudPlatform(), stack.owner, stack.platformVariant,
                location)
        val cloudCredential = credentialConverter!!.convert(stack.credential)
        val instanceGroupName = extractInstanceGroupName(payload, variables)
        val instanceIds = extractInstanceIds(payload, variables, stack)
        val adjustment = extractAdjustment(payload, variables)
        val cloudStack = cloudStackConverter!!.convertForDownscale(stack, instanceIds)
        return StackScalingFlowContext(flowId, stack, cloudContext, cloudCredential, cloudStack, instanceGroupName, instanceIds, adjustment)
    }

    private fun extractAdjustment(payload: P, variables: MutableMap<Any, Any>): Int? {
        if (payload is StackScaleTriggerEvent) {
            variables.put(ADJUSTMENT, payload.adjustment)
            return payload.adjustment
        }
        return getAdjustment(variables)
    }

    private fun extractInstanceIds(payload: P, variables: MutableMap<Any, Any>, stack: Stack): Set<String> {
        if (payload is StackScaleTriggerEvent) {
            val unusedInstanceIds = stackScalingService!!.getUnusedInstanceIds(payload.instanceGroup, payload.adjustment, stack)
            val instanceIds = HashSet(unusedInstanceIds.keys)
            variables.put(INSTANCEIDS, instanceIds)
            return instanceIds
        }
        return getInstanceIds(variables)
    }

    private fun extractInstanceGroupName(payload: P, variables: MutableMap<Any, Any>): String {
        if (payload is StackScaleTriggerEvent) {
            variables.put(INSTANCEGROUPNAME, payload.instanceGroup)
            return payload.instanceGroup
        }
        return getInstanceGroupName(variables)
    }

    override fun getFailurePayload(payload: P, flowContext: Optional<StackScalingFlowContext>, ex: Exception): Any {
        return StackFailureEvent(payload.stackId, ex)
    }

    protected fun getInstanceGroupName(variables: Map<Any, Any>): String {
        return variables[INSTANCEGROUPNAME] as String
    }

    protected fun getInstanceIds(variables: Map<Any, Any>): Set<String> {
        return variables[INSTANCEIDS] as Set<String>
    }

    protected fun getAdjustment(variables: Map<Any, Any>): Int? {
        return variables[ADJUSTMENT] as Int
    }

    companion object {
        protected val INSTANCEGROUPNAME = "INSTANCEGROUPNAME"
        protected val INSTANCEIDS = "INSTANCEIDS"
        private val ADJUSTMENT = "ADJUSTMENT"
    }
}
