package com.sequenceiq.cloudbreak.core.flow2.stack.instance.termination

import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone
import com.sequenceiq.cloudbreak.cloud.model.Location.location
import com.sequenceiq.cloudbreak.cloud.model.Region.region
import java.util.Optional

import javax.inject.Inject

import org.springframework.statemachine.StateContext

import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.event.InstancePayload
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance
import com.sequenceiq.cloudbreak.cloud.model.CloudResource
import com.sequenceiq.cloudbreak.cloud.model.CloudStack
import com.sequenceiq.cloudbreak.cloud.model.Location
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter
import com.sequenceiq.cloudbreak.converter.spi.InstanceMetaDataToCloudInstanceConverter
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter
import com.sequenceiq.cloudbreak.converter.spi.StackToCloudStackConverter
import com.sequenceiq.cloudbreak.core.flow2.AbstractAction
import com.sequenceiq.cloudbreak.domain.InstanceMetaData
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.logger.MDCBuilder
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository
import com.sequenceiq.cloudbreak.service.stack.StackService

internal abstract class AbstractInstanceTerminationAction<P : InstancePayload> protected constructor(payloadClass: Class<P>) : AbstractAction<InstanceTerminationState, InstanceTerminationEvent, InstanceTerminationContext, P>(payloadClass) {

    @Inject
    private val stackService: StackService? = null

    @Inject
    private val instanceMetaDataRepository: InstanceMetaDataRepository? = null

    @Inject
    private val credentialConverter: CredentialToCloudCredentialConverter? = null

    @Inject
    private val cloudStackConverter: StackToCloudStackConverter? = null

    @Inject
    private val cloudResourceConverter: ResourceToCloudResourceConverter? = null

    @Inject
    private val metadataConverter: InstanceMetaDataToCloudInstanceConverter? = null

    override fun createFlowContext(flowId: String, stateContext: StateContext<InstanceTerminationState, InstanceTerminationEvent>,
                                   payload: P): InstanceTerminationContext {
        val stack = stackService!!.getById(payload.stackId)
        MDCBuilder.buildMdcContext(stack)
        val location = Companion.location(Companion.region(stack.region), Companion.availabilityZone(stack.availabilityZone))
        val cloudContext = CloudContext(stack.id, stack.name, stack.cloudPlatform(), stack.owner, stack.platformVariant,
                location)
        val cloudCredential = credentialConverter!!.convert(stack.credential)
        val instanceId = payload.instanceId
        val cloudStack = cloudStackConverter!!.convertForTermination(stack, instanceId)
        val cloudResources = cloudResourceConverter!!.convert(stack.resources)
        val instanceMetaData = instanceMetaDataRepository!!.findByInstanceId(stack.id, instanceId)
        val cloudInstance = metadataConverter!!.convert(instanceMetaData)
        return InstanceTerminationContext(flowId, stack, cloudContext, cloudCredential, cloudStack, cloudResources, cloudInstance, instanceMetaData)
    }

    override fun getFailurePayload(payload: P, flowContext: Optional<InstanceTerminationContext>, ex: Exception): Any {
        return StackFailureEvent(payload.stackId, ex)
    }
}
