package com.sequenceiq.cloudbreak.core.flow2.stack.start

import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone
import com.sequenceiq.cloudbreak.cloud.model.Location.location
import com.sequenceiq.cloudbreak.cloud.model.Region.region

import java.util.ArrayList
import java.util.Optional

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.statemachine.StateContext
import org.springframework.statemachine.action.Action

import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.event.Payload
import com.sequenceiq.cloudbreak.cloud.event.Selectable
import com.sequenceiq.cloudbreak.cloud.event.instance.CollectMetadataRequest
import com.sequenceiq.cloudbreak.cloud.event.instance.CollectMetadataResult
import com.sequenceiq.cloudbreak.cloud.event.instance.StartInstancesRequest
import com.sequenceiq.cloudbreak.cloud.event.instance.StartInstancesResult
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance
import com.sequenceiq.cloudbreak.cloud.model.CloudResource
import com.sequenceiq.cloudbreak.cloud.model.Location
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter
import com.sequenceiq.cloudbreak.converter.spi.InstanceMetaDataToCloudInstanceConverter
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter
import com.sequenceiq.cloudbreak.converter.spi.StackToCloudStackConverter
import com.sequenceiq.cloudbreak.core.flow2.AbstractAction
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext
import com.sequenceiq.cloudbreak.domain.InstanceMetaData
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.logger.MDCBuilder
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService
import com.sequenceiq.cloudbreak.service.stack.StackService

@Configuration
class StackStartActions {

    @Inject
    private val cloudStackConverter: StackToCloudStackConverter? = null
    @Inject
    private val cloudInstanceConverter: InstanceMetaDataToCloudInstanceConverter? = null
    @Inject
    private val cloudResourceConverter: ResourceToCloudResourceConverter? = null
    @Inject
    private val stackStartStopService: StackStartStopService? = null

    @Bean(name = "START_STATE")
    fun stackStartAction(): Action<Any, Any> {
        return object : AbstractStackStartAction<StackEvent>(StackEvent::class.java) {
            @Throws(Exception::class)
            override fun doExecute(context: StackStartStopContext, payload: StackEvent, variables: Map<Any, Any>) {
                stackStartStopService!!.startStackStart(context)
                sendEvent(context)
            }

            override fun createRequest(context: StackStartStopContext): Selectable {
                LOGGER.info("Assembling start request for stack: {}", context.stack)
                val instances = cloudInstanceConverter!!.convert(context.stack.instanceMetaDataAsList)
                val resources = cloudResourceConverter!!.convert(context.stack.resources)
                return StartInstancesRequest(context.cloudContext, context.cloudCredential, resources, instances)
            }
        }
    }

    @Bean(name = "COLLECTING_METADATA")
    fun collectingMetadataAction(): Action<Any, Any> {
        return object : AbstractStackStartAction<StartInstancesResult>(StartInstancesResult::class.java) {
            @Throws(Exception::class)
            override fun doExecute(context: StackStartStopContext, payload: StartInstancesResult, variables: Map<Any, Any>) {
                sendEvent(context)
            }

            override fun createRequest(context: StackStartStopContext): Selectable {
                val cloudInstances = cloudStackConverter!!.buildInstances(context.stack)
                val cloudResources = cloudResourceConverter!!.convert(context.stack.resources)
                return CollectMetadataRequest(context.cloudContext, context.cloudCredential, cloudResources, cloudInstances)
            }
        }
    }

    @Bean(name = "START_FINISHED_STATE")
    fun startFinishedAction(): Action<Any, Any> {
        return object : AbstractStackStartAction<CollectMetadataResult>(CollectMetadataResult::class.java) {
            @Throws(Exception::class)
            override fun doExecute(context: StackStartStopContext, payload: CollectMetadataResult, variables: Map<Any, Any>) {
                stackStartStopService!!.finishStackStart(context.stack, payload.results)
                sendEvent(context)
            }

            override fun createRequest(context: StackStartStopContext): Selectable {
                return StackEvent(StackStartEvent.START_FINALIZED_EVENT.stringRepresentation(), context.stack.id)
            }
        }
    }

    @Bean(name = "START_FAILED_STATE")
    fun stackStartFailedAction(): Action<Any, Any> {
        return object : AbstractStackFailureAction<StackStartState, StackStartEvent>() {
            @Throws(Exception::class)
            override fun doExecute(context: StackFailureContext, payload: StackFailureEvent, variables: Map<Any, Any>) {
                stackStartStopService!!.handleStackStartError(context.stack, payload)
                sendEvent(context)
            }

            override fun createRequest(context: StackFailureContext): Selectable {
                return StackEvent(StackStartEvent.START_FAIL_HANDLED_EVENT.stringRepresentation(), context.stack.id)
            }
        }
    }

    private abstract class AbstractStackStartAction<P : Payload> protected constructor(payloadClass: Class<P>) : AbstractAction<StackStartState, StackStartEvent, StackStartStopContext, P>(payloadClass) {
        @Inject
        private val stackService: StackService? = null
        @Inject
        private val instanceMetaDataRepository: InstanceMetaDataRepository? = null
        @Inject
        private val credentialConverter: CredentialToCloudCredentialConverter? = null
        @Inject
        private val cloudInstanceConverter: InstanceMetaDataToCloudInstanceConverter? = null
        @Inject
        private val cloudResourceConverter: ResourceToCloudResourceConverter? = null
        @Inject
        private val messagesService: CloudbreakMessagesService? = null
        @Inject
        private val cloudbreakEventService: CloudbreakEventService? = null

        override fun createFlowContext(flowId: String, stateContext: StateContext<StackStartState, StackStartEvent>, payload: P): StackStartStopContext {
            val stackId = payload.stackId
            val stack = stackService!!.getById(stackId)
            MDCBuilder.buildMdcContext(stack)
            val instances = ArrayList(instanceMetaDataRepository!!.findNotTerminatedForStack(stackId))
            val location = Companion.location(Companion.region(stack.region), Companion.availabilityZone(stack.availabilityZone))
            val cloudContext = CloudContext(stack.id, stack.name, stack.cloudPlatform(), stack.owner, stack.platformVariant,
                    location)
            val cloudCredential = credentialConverter!!.convert(stack.credential)
            return StackStartStopContext(flowId, stack, instances, cloudContext, cloudCredential)
        }

        override fun getFailurePayload(payload: P, flowContext: Optional<StackStartStopContext>, ex: Exception): Any {
            return StackFailureEvent(payload.stackId, ex)
        }

        companion object {
            private val LOGGER = LoggerFactory.getLogger(AbstractStackStartAction<Payload>::class.java)
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(StackStartActions::class.java)
    }
}
