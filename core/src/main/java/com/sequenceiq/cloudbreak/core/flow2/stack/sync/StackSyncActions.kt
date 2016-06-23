package com.sequenceiq.cloudbreak.core.flow2.stack.sync

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

import com.sequenceiq.cloudbreak.api.model.Status
import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.event.Payload
import com.sequenceiq.cloudbreak.cloud.event.Selectable
import com.sequenceiq.cloudbreak.cloud.event.resource.GetInstancesStateRequest
import com.sequenceiq.cloudbreak.cloud.event.resource.GetInstancesStateResult
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance
import com.sequenceiq.cloudbreak.cloud.model.Location
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter
import com.sequenceiq.cloudbreak.converter.spi.InstanceMetaDataToCloudInstanceConverter
import com.sequenceiq.cloudbreak.core.flow2.AbstractAction
import com.sequenceiq.cloudbreak.core.flow2.event.StackSyncTriggerEvent
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction
import com.sequenceiq.cloudbreak.core.flow2.stack.FlowMessageService
import com.sequenceiq.cloudbreak.core.flow2.stack.Msg
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext
import com.sequenceiq.cloudbreak.domain.InstanceMetaData
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.logger.MDCBuilder
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository
import com.sequenceiq.cloudbreak.service.stack.StackService
import com.sequenceiq.cloudbreak.service.stack.flow.StackSyncService

@Configuration
class StackSyncActions {

    @Inject
    private val cloudInstanceConverter: InstanceMetaDataToCloudInstanceConverter? = null
    @Inject
    private val stackSyncService: StackSyncService? = null
    @Inject
    private val flowMessageService: FlowMessageService? = null

    @Bean(name = "SYNC_STATE")
    fun stackSyncAction(): Action<Any, Any> {
        return object : AbstractStackSyncAction<StackSyncTriggerEvent>(StackSyncTriggerEvent::class.java) {
            override fun prepareExecution(payload: StackSyncTriggerEvent, variables: MutableMap<Any, Any>) {
                variables.put(StackSyncActions.AbstractStackSyncAction.STATUS_UPDATE_ENABLED, payload.statusUpdateEnabled)
            }

            @Throws(Exception::class)
            override fun doExecute(context: StackSyncContext, payload: StackSyncTriggerEvent, variables: Map<Any, Any>) {
                sendEvent(context)
            }

            override fun createRequest(context: StackSyncContext): Selectable {
                val cloudInstances = cloudInstanceConverter!!.convert(context.instanceMetaData)
                return GetInstancesStateRequest<GetInstancesStateResult>(context.cloudContext, context.cloudCredential, cloudInstances)
            }
        }
    }

    @Bean(name = "SYNC_FINISHED_STATE")
    fun stackSyncFinishedAction(): Action<Any, Any> {
        return object : AbstractStackSyncAction<GetInstancesStateResult>(GetInstancesStateResult::class.java) {
            @Throws(Exception::class)
            override fun doExecute(context: StackSyncContext, payload: GetInstancesStateResult, variables: Map<Any, Any>) {
                stackSyncService!!.updateInstances(context.stack, context.instanceMetaData, payload.statuses, context.isStatusUpdateEnabled!!)
                sendEvent(context)
            }

            override fun createRequest(context: StackSyncContext): Selectable {
                return StackEvent(StackSyncEvent.SYNC_FINALIZED_EVENT.stringRepresentation(), context.stack.id)
            }
        }
    }

    @Bean(name = "SYNC_FAILED_STATE")
    fun stackSyncFailedAction(): Action<Any, Any> {
        return object : AbstractStackFailureAction<StackSyncState, StackSyncEvent>() {
            @Throws(Exception::class)
            override fun doExecute(context: StackFailureContext, payload: StackFailureEvent, variables: Map<Any, Any>) {
                LOGGER.error("Error during Stack synchronization flow:", payload.exception)
                flowMessageService!!.fireEventAndLog(context.stack.id, Msg.STACK_SYNC_INSTANCE_STATUS_COULDNT_DETERMINE, Status.AVAILABLE.name)
                sendEvent(context)
            }

            override fun createRequest(context: StackFailureContext): Selectable {
                return StackEvent(StackSyncEvent.SYNC_FAIL_HANDLED_EVENT.stringRepresentation(), context.stack.id)
            }
        }
    }

    private abstract class AbstractStackSyncAction<P : Payload> protected constructor(payloadClass: Class<P>) : AbstractAction<StackSyncState, StackSyncEvent, StackSyncContext, P>(payloadClass) {

        @Inject
        private val stackService: StackService? = null
        @Inject
        private val instanceMetaDataRepository: InstanceMetaDataRepository? = null
        @Inject
        private val credentialConverter: CredentialToCloudCredentialConverter? = null
        @Inject
        private val cloudInstanceConverter: InstanceMetaDataToCloudInstanceConverter? = null

        override fun createFlowContext(flowId: String, stateContext: StateContext<StackSyncState, StackSyncEvent>, payload: P): StackSyncContext {
            val variables = stateContext.extendedState.variables
            val stackId = payload.stackId
            val stack = stackService!!.getById(stackId)
            MDCBuilder.buildMdcContext(stack)
            val instances = ArrayList(instanceMetaDataRepository!!.findNotTerminatedForStack(stackId))
            val location = Companion.location(Companion.region(stack.region), Companion.availabilityZone(stack.availabilityZone))
            val cloudContext = CloudContext(stack.id, stack.name, stack.cloudPlatform(), stack.owner, stack.platformVariant,
                    location)
            val cloudCredential = credentialConverter!!.convert(stack.credential)
            return StackSyncContext(flowId, stack, instances, cloudContext, cloudCredential, isStatusUpdateEnabled(variables))
        }

        override fun getFailurePayload(payload: P, flowContext: Optional<StackSyncContext>, ex: Exception): Any {
            return StackFailureEvent(payload.stackId, ex)
        }

        private fun isStatusUpdateEnabled(variables: Map<Any, Any>): Boolean? {
            return variables[STATUS_UPDATE_ENABLED] as Boolean
        }

        companion object {
            internal val STATUS_UPDATE_ENABLED = "STATUS_UPDATE_ENABLED"
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(StackSyncActions::class.java)
    }
}
