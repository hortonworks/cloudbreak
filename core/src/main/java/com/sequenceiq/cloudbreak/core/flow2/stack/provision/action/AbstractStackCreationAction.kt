package com.sequenceiq.cloudbreak.core.flow2.stack.provision.action

import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone
import com.sequenceiq.cloudbreak.cloud.model.Location.location
import com.sequenceiq.cloudbreak.cloud.model.Region.region

import java.util.Optional

import javax.inject.Inject

import org.springframework.statemachine.StateContext

import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.event.Payload
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential
import com.sequenceiq.cloudbreak.cloud.model.CloudStack
import com.sequenceiq.cloudbreak.cloud.model.Location
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter
import com.sequenceiq.cloudbreak.converter.spi.StackToCloudStackConverter
import com.sequenceiq.cloudbreak.core.flow2.AbstractAction
import com.sequenceiq.cloudbreak.core.flow2.stack.StackContext
import com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent
import com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationState
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.logger.MDCBuilder
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent
import com.sequenceiq.cloudbreak.service.stack.StackService

abstract class AbstractStackCreationAction<P : Payload> protected constructor(payloadClass: Class<P>) : AbstractAction<StackCreationState, StackCreationEvent, StackContext, P>(payloadClass) {
    @Inject
    private val stackService: StackService? = null
    @Inject
    private val cloudStackConverter: StackToCloudStackConverter? = null
    @Inject
    private val credentialConverter: CredentialToCloudCredentialConverter? = null

    override fun createFlowContext(flowId: String, stateContext: StateContext<StackCreationState, StackCreationEvent>, payload: P): StackContext {
        val stack = stackService!!.getById(payload.stackId)
        // TODO LogAspect!!
        MDCBuilder.buildMdcContext(stack)
        val location = Companion.location(Companion.region(stack.region), Companion.availabilityZone(stack.availabilityZone))
        val cloudContext = CloudContext(stack.id, stack.name, stack.cloudPlatform(), stack.owner, stack.platformVariant,
                location)
        val cloudCredential = credentialConverter!!.convert(stack.credential)
        val cloudStack = cloudStackConverter!!.convert(stack)
        return StackContext(flowId, stack, cloudContext, cloudCredential, cloudStack)
    }

    override fun getFailurePayload(payload: P, flowContext: Optional<StackContext>, ex: Exception): Any {
        return StackFailureEvent(payload.stackId, ex)
    }
}
