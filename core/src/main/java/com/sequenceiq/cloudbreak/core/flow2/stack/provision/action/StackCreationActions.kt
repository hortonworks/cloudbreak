package com.sequenceiq.cloudbreak.core.flow2.stack.provision.action

import com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackProvisionConstants.START_DATE

import java.util.Date

import javax.inject.Inject

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.statemachine.action.Action

import com.google.common.base.Optional
import com.sequenceiq.cloudbreak.cloud.event.Selectable
import com.sequenceiq.cloudbreak.cloud.event.instance.CollectMetadataRequest
import com.sequenceiq.cloudbreak.cloud.event.instance.CollectMetadataResult
import com.sequenceiq.cloudbreak.cloud.event.instance.GetSSHFingerprintsRequest
import com.sequenceiq.cloudbreak.cloud.event.instance.GetSSHFingerprintsResult
import com.sequenceiq.cloudbreak.cloud.event.resource.LaunchStackRequest
import com.sequenceiq.cloudbreak.cloud.event.resource.LaunchStackResult
import com.sequenceiq.cloudbreak.cloud.event.setup.PrepareImageRequest
import com.sequenceiq.cloudbreak.cloud.event.setup.SetupRequest
import com.sequenceiq.cloudbreak.cloud.event.setup.SetupResult
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance
import com.sequenceiq.cloudbreak.cloud.model.CloudResource
import com.sequenceiq.cloudbreak.cloud.model.CloudStack
import com.sequenceiq.cloudbreak.cloud.model.Image
import com.sequenceiq.cloudbreak.converter.spi.InstanceMetaDataToCloudInstanceConverter
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter
import com.sequenceiq.cloudbreak.converter.spi.StackToCloudStackConverter
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction
import com.sequenceiq.cloudbreak.core.flow2.stack.StackContext
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext
import com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent
import com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationState
import com.sequenceiq.cloudbreak.domain.FailurePolicy
import com.sequenceiq.cloudbreak.domain.InstanceMetaData
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.BootstrapMachinesRequest
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.BootstrapMachinesSuccess
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.HostMetadataSetupRequest
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.HostMetadataSetupSuccess
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException
import com.sequenceiq.cloudbreak.service.image.ImageService

@Configuration
class StackCreationActions {
    @Inject
    private val imageService: ImageService? = null
    @Inject
    private val cloudStackConverter: StackToCloudStackConverter? = null
    @Inject
    private val stackCreationService: StackCreationService? = null
    @Inject
    private val cloudResourceConverter: ResourceToCloudResourceConverter? = null
    @Inject
    private val metadataConverter: InstanceMetaDataToCloudInstanceConverter? = null

    @Bean(name = "SETUP_STATE")
    fun provisioningSetupAction(): Action<Any, Any> {
        return object : AbstractStackCreationAction<StackEvent>(StackEvent::class.java) {
            @Throws(Exception::class)
            override fun doExecute(context: StackContext, payload: StackEvent, variables: Map<Any, Any>) {
                sendEvent(context)
            }

            override fun createRequest(context: StackContext): Selectable {
                return SetupRequest<SetupResult>(context.cloudContext, context.cloudCredential, context.cloudStack)
            }
        }
    }

    @Bean(name = "IMAGESETUP_STATE")
    fun prepareImageAction(): Action<Any, Any> {
        return object : AbstractStackCreationAction<SetupResult>(SetupResult::class.java) {
            @Throws(Exception::class)
            override fun doExecute(context: StackContext, payload: SetupResult, variables: Map<Any, Any>) {
                sendEvent(context)
            }

            override fun createRequest(context: StackContext): Selectable {
                try {
                    val cloudStack = cloudStackConverter!!.convert(context.stack)
                    val image = imageService!!.getImage(context.cloudContext.id)
                    return PrepareImageRequest<Any>(context.cloudContext, context.cloudCredential, cloudStack, image)
                } catch (e: CloudbreakImageNotFoundException) {
                    throw CloudbreakServiceException(e)
                }

            }
        }
    }

    @Bean(name = "START_PROVISIONING_STATE")
    fun startProvisioningAction(): Action<Any, Any> {
        return object : AbstractStackCreationAction<StackEvent>(StackEvent::class.java) {
            @Throws(Exception::class)
            override fun doExecute(context: StackContext, payload: StackEvent, variables: MutableMap<Any, Any>) {
                variables.put(START_DATE, Date())
                stackCreationService!!.startProvisioning(context)
                sendEvent(context)
            }

            override fun createRequest(context: StackContext): Selectable {
                val policy = Optional.fromNullable(context.stack.failurePolicy).or(FailurePolicy())
                return LaunchStackRequest(context.cloudContext, context.cloudCredential, context.cloudStack,
                        policy.adjustmentType, policy.threshold)
            }
        }
    }

    @Bean(name = "PROVISIONING_FINISHED_STATE")
    fun provisioningFinishedAction(): Action<Any, Any> {
        return object : AbstractStackCreationAction<LaunchStackResult>(LaunchStackResult::class.java) {
            @Throws(Exception::class)
            override fun doExecute(context: StackContext, payload: LaunchStackResult, variables: Map<Any, Any>) {
                val stack = stackCreationService!!.provisioningFinished(context, payload, variables)
                val newContext = StackContext(context.flowId, stack, context.cloudContext,
                        context.cloudCredential, context.cloudStack)
                sendEvent(newContext)
            }

            override fun createRequest(context: StackContext): Selectable {
                val cloudInstances = cloudStackConverter!!.buildInstances(context.stack)
                val cloudResources = cloudResourceConverter!!.convert(context.stack.resources)
                return CollectMetadataRequest(context.cloudContext, context.cloudCredential, cloudResources, cloudInstances)
            }
        }
    }

    @Bean(name = "COLLECTMETADATA_STATE")
    fun collectMetadataAction(): Action<Any, Any> {
        return object : AbstractStackCreationAction<CollectMetadataResult>(CollectMetadataResult::class.java) {
            @Throws(Exception::class)
            override fun doExecute(context: StackContext, payload: CollectMetadataResult, variables: Map<Any, Any>) {
                val stack = stackCreationService!!.setupMetadata(context, payload)
                val newContext = StackContext(context.flowId, stack, context.cloudContext, context.cloudCredential,
                        context.cloudStack)
                sendEvent(newContext)
            }

            override fun createRequest(context: StackContext): Selectable {
                val gatewayMetaData = context.stack.gatewayInstanceGroup.instanceMetaData.iterator().next()
                val gatewayInstance = metadataConverter!!.convert(gatewayMetaData)
                return GetSSHFingerprintsRequest<GetSSHFingerprintsResult>(context.cloudContext, context.cloudCredential, gatewayInstance)
            }
        }
    }

    @Bean(name = "TLS_SETUP_STATE")
    fun tlsSetupAction(): Action<Any, Any> {
        return object : AbstractStackCreationAction<GetSSHFingerprintsResult>(GetSSHFingerprintsResult::class.java) {
            @Throws(Exception::class)
            override fun doExecute(context: StackContext, payload: GetSSHFingerprintsResult, variables: Map<Any, Any>) {
                stackCreationService!!.setupTls(context, payload)
                sendEvent(context)
            }

            override fun createRequest(context: StackContext): Selectable {
                return StackEvent(StackCreationEvent.BOOTSTRAP_MACHINES_EVENT.stringRepresentation(), context.stack.id)
            }
        }
    }

    @Bean(name = "BOOTSTRAPING_MACHINES_STATE")
    fun bootstrappingMachinesAction(): Action<Any, Any> {
        return object : AbstractStackCreationAction<StackEvent>(StackEvent::class.java) {
            @Throws(Exception::class)
            override fun doExecute(context: StackContext, payload: StackEvent, variables: Map<Any, Any>) {
                stackCreationService!!.bootstrappingMachines(context.stack)
                sendEvent(context)
            }

            override fun createRequest(context: StackContext): Selectable {
                return BootstrapMachinesRequest(context.stack.id)
            }
        }
    }

    @Bean(name = "COLLECTING_HOST_METADATA_STATE")
    fun collectingHostMetadataAction(): Action<Any, Any> {
        return object : AbstractStackCreationAction<BootstrapMachinesSuccess>(BootstrapMachinesSuccess::class.java) {
            @Throws(Exception::class)
            override fun doExecute(context: StackContext, payload: BootstrapMachinesSuccess, variables: Map<Any, Any>) {
                stackCreationService!!.collectingHostMetadata(context.stack)
                sendEvent(context)
            }

            override fun createRequest(context: StackContext): Selectable {
                return HostMetadataSetupRequest(context.stack.id)
            }
        }
    }

    @Bean(name = "STACK_CREATION_FINISHED_STATE")
    fun stackCreationFinishedAction(): Action<Any, Any> {
        return object : AbstractStackCreationAction<HostMetadataSetupSuccess>(HostMetadataSetupSuccess::class.java) {
            @Throws(Exception::class)
            override fun doExecute(context: StackContext, payload: HostMetadataSetupSuccess, variables: Map<Any, Any>) {
                stackCreationService!!.stackCreationFinished(context.stack)
                sendEvent(context)
            }

            override fun createRequest(context: StackContext): Selectable {
                return StackEvent(StackCreationEvent.STACK_CREATION_FINISHED_EVENT.stringRepresentation(), context.stack.id)
            }
        }
    }

    @Bean(name = "STACK_CREATION_FAILED_STATE")
    fun stackCreationFailureAction(): Action<Any, Any> {
        return object : AbstractStackFailureAction<StackCreationState, StackCreationEvent>() {
            @Throws(Exception::class)
            override fun doExecute(context: StackFailureContext, payload: StackFailureEvent, variables: Map<Any, Any>) {
                stackCreationService!!.handleStackCreationFailure(context.stack, payload.exception)
                sendEvent(context)
            }

            override fun createRequest(context: StackFailureContext): Selectable {
                return StackEvent(StackCreationEvent.STACKCREATION_FAILURE_HANDLED_EVENT.stringRepresentation(), context.stack.id)
            }
        }
    }
}
