package com.sequenceiq.cloudbreak.core.flow2.stack.stop

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
import com.sequenceiq.cloudbreak.cloud.event.instance.StopInstancesRequest
import com.sequenceiq.cloudbreak.cloud.event.instance.StopInstancesResult
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance
import com.sequenceiq.cloudbreak.cloud.model.CloudResource
import com.sequenceiq.cloudbreak.cloud.model.Location
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter
import com.sequenceiq.cloudbreak.converter.spi.InstanceMetaDataToCloudInstanceConverter
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter
import com.sequenceiq.cloudbreak.core.flow2.AbstractAction
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext
import com.sequenceiq.cloudbreak.core.flow2.stack.start.StackStartStopContext
import com.sequenceiq.cloudbreak.core.flow2.stack.start.StackStartStopService
import com.sequenceiq.cloudbreak.domain.InstanceMetaData
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.logger.MDCBuilder
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository
import com.sequenceiq.cloudbreak.service.stack.StackService

@Configuration
class StackStopActions {
    @Inject
    private val cloudInstanceConverter: InstanceMetaDataToCloudInstanceConverter? = null
    @Inject
    private val cloudResourceConverter: ResourceToCloudResourceConverter? = null
    @Inject
    private val stackStartStopService: StackStartStopService? = null

    @Bean(name = "STOP_STATE")
    fun stackStopAction(): Action<Any, Any> {
        return object : AbstractStackStopAction<StackEvent>(StackEvent::class.java) {
            @Throws(Exception::class)
            override fun doExecute(context: StackStartStopContext, payload: StackEvent, variables: Map<Any, Any>) {
                stackStartStopService!!.startStackStop(context)
                sendEvent(context)
            }

            override fun createRequest(context: StackStartStopContext): Selectable {
                val cloudInstances = cloudInstanceConverter!!.convert(context.instanceMetaData)
                val cloudResources = cloudResourceConverter!!.convert(context.stack.resources)
                return StopInstancesRequest<StopInstancesResult>(context.cloudContext, context.cloudCredential, cloudResources, cloudInstances)
            }
        }
    }

    @Bean(name = "STOP_FINISHED_STATE")
    fun stackStopFinishedAction(): Action<Any, Any> {
        return object : AbstractStackStopAction<StopInstancesResult>(StopInstancesResult::class.java) {
            @Throws(Exception::class)
            override fun doExecute(context: StackStartStopContext, payload: StopInstancesResult, variables: Map<Any, Any>) {
                stackStartStopService!!.finishStackStop(context)
                sendEvent(context)
            }

            override fun createRequest(context: StackStartStopContext): Selectable {
                return StackEvent(StackStopEvent.STOP_FINALIZED_EVENT.stringRepresentation(), context.stack.id)
            }
        }
    }

    @Bean(name = "STOP_FAILED_STATE")
    fun stackStopFailedAction(): Action<Any, Any> {
        return object : AbstractStackFailureAction<StackStopState, StackStopEvent>() {
            @Throws(Exception::class)
            override fun doExecute(context: StackFailureContext, payload: StackFailureEvent, variables: Map<Any, Any>) {
                stackStartStopService!!.handleStackStopError(context.stack, payload)
                sendEvent(context)
            }

            override fun createRequest(context: StackFailureContext): Selectable {
                return StackEvent(StackStopEvent.STOP_FAIL_HANDLED_EVENT.stringRepresentation(), context.stack.id)
            }
        }
    }

    private abstract class AbstractStackStopAction<P : Payload> protected constructor(payloadClass: Class<P>) : AbstractAction<StackStopState, StackStopEvent, StackStartStopContext, P>(payloadClass) {
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

        override fun createFlowContext(flowId: String, stateContext: StateContext<StackStopState, StackStopEvent>, payload: P): StackStartStopContext {
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
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(StackStopActions::class.java)
    }
}
