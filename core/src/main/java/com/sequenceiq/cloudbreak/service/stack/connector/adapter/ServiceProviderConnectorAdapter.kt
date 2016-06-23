package com.sequenceiq.cloudbreak.service.stack.connector.adapter

import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone
import com.sequenceiq.cloudbreak.cloud.model.Location.location
import com.sequenceiq.cloudbreak.cloud.model.Region.region
import java.lang.String.format

import java.util.ArrayList

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.cloud.PlatformParameters
import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus
import com.sequenceiq.cloudbreak.cloud.event.platform.CheckPlatformVariantRequest
import com.sequenceiq.cloudbreak.cloud.event.platform.CheckPlatformVariantResult
import com.sequenceiq.cloudbreak.cloud.event.platform.PlatformParameterRequest
import com.sequenceiq.cloudbreak.cloud.event.platform.PlatformParameterResult
import com.sequenceiq.cloudbreak.cloud.event.resource.DownscaleStackRequest
import com.sequenceiq.cloudbreak.cloud.event.resource.DownscaleStackResult
import com.sequenceiq.cloudbreak.cloud.event.resource.TerminateStackRequest
import com.sequenceiq.cloudbreak.cloud.event.resource.TerminateStackResult
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance
import com.sequenceiq.cloudbreak.cloud.model.CloudResource
import com.sequenceiq.cloudbreak.cloud.model.CloudStack
import com.sequenceiq.cloudbreak.cloud.model.Location
import com.sequenceiq.cloudbreak.cloud.model.Variant
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter
import com.sequenceiq.cloudbreak.converter.spi.InstanceMetaDataToCloudInstanceConverter
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter
import com.sequenceiq.cloudbreak.converter.spi.StackToCloudStackConverter
import com.sequenceiq.cloudbreak.domain.Credential
import com.sequenceiq.cloudbreak.domain.InstanceGroup
import com.sequenceiq.cloudbreak.domain.InstanceMetaData
import com.sequenceiq.cloudbreak.domain.Resource
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.service.stack.connector.OperationException

import reactor.bus.Event
import reactor.bus.EventBus

@Component
class ServiceProviderConnectorAdapter {

    @Inject
    private val eventBus: EventBus? = null
    @Inject
    private val cloudStackConverter: StackToCloudStackConverter? = null
    @Inject
    private val metadataConverter: InstanceMetaDataToCloudInstanceConverter? = null
    @Inject
    private val credentialConverter: CredentialToCloudCredentialConverter? = null
    @Inject
    private val cloudResourceConverter: ResourceToCloudResourceConverter? = null

    fun removeInstances(stack: Stack, instanceIds: Set<String>, instanceGroup: String): Set<String> {
        LOGGER.debug("Assembling downscale stack event for stack: {}", stack)
        val location = Companion.location(Companion.region(stack.region), Companion.availabilityZone(stack.availabilityZone))
        val cloudContext = CloudContext(stack.id, stack.name, stack.cloudPlatform(), stack.owner, stack.platformVariant,
                location)
        val cloudCredential = credentialConverter!!.convert(stack.credential)
        val resources = cloudResourceConverter!!.convert(stack.resources)
        val instances = ArrayList<CloudInstance>()
        val group = stack.getInstanceGroupByInstanceGroupName(instanceGroup)
        for (metaData in group.allInstanceMetaData) {
            if (instanceIds.contains(metaData.instanceId)) {
                val cloudInstance = metadataConverter!!.convert(metaData)
                instances.add(cloudInstance)
            }
        }
        val cloudStack = cloudStackConverter!!.convertForDownscale(stack, instanceIds)
        val downscaleRequest = DownscaleStackRequest<DownscaleStackResult>(cloudContext,
                cloudCredential, cloudStack, resources, instances)
        LOGGER.info("Triggering downscale stack event: {}", downscaleRequest)
        eventBus!!.notify(downscaleRequest.selector(), Event.wrap(downscaleRequest))
        try {
            val res = downscaleRequest.await()
            LOGGER.info("Downscale stack result: {}", res)
            if (res.status == EventStatus.FAILED) {
                LOGGER.error("Failed to downscale the stack", res.errorDetails)
                throw OperationException(res.errorDetails)
            }
            return instanceIds
        } catch (e: InterruptedException) {
            LOGGER.error("Error while downscaling the stack", e)
            throw OperationException(e)
        }

    }

    fun deleteStack(stack: Stack, credential: Credential) {
        LOGGER.debug("Assembling terminate stack event for stack: {}", stack)
        val location = Companion.location(Companion.region(stack.region), Companion.availabilityZone(stack.availabilityZone))
        val cloudContext = CloudContext(stack.id, stack.name, stack.cloudPlatform(), stack.owner, stack.platformVariant,
                location)
        val cloudCredential = credentialConverter!!.convert(stack.credential)
        val resources = cloudResourceConverter!!.convert(stack.resources)
        val cloudStack = cloudStackConverter!!.convert(stack)
        val terminateRequest = TerminateStackRequest<TerminateStackResult>(cloudContext, cloudStack, cloudCredential, resources)
        LOGGER.info("Triggering terminate stack event: {}", terminateRequest)
        eventBus!!.notify(terminateRequest.selector(), Event.wrap(terminateRequest))
        try {
            val res = terminateRequest.await()
            LOGGER.info("Terminate stack result: {}", res)
            if (res.status == EventStatus.FAILED) {
                if (res.errorDetails != null) {
                    LOGGER.error("Failed to terminate the stack", res.errorDetails)
                    throw OperationException(res.errorDetails)
                }
                throw OperationException(format("Failed to terminate the stack: %s due to %s", cloudContext, res.statusReason))
            }
        } catch (e: InterruptedException) {
            LOGGER.error("Error while terminating the stack", e)
            throw OperationException(e)
        }

    }

    fun rollback(stack: Stack, resourceSet: Set<Resource>) {
        LOGGER.info("Rollback the whole stack for {}", stack.id)
        deleteStack(stack, stack.credential)
    }

    fun getPlatformParameters(stack: Stack): PlatformParameters {
        LOGGER.debug("Get platform parameters for: {}", stack)
        val location = Companion.location(Companion.region(stack.region), Companion.availabilityZone(stack.availabilityZone))
        val cloudContext = CloudContext(stack.id, stack.name, stack.cloudPlatform(), stack.owner, stack.platformVariant,
                location)
        val cloudCredential = credentialConverter!!.convert(stack.credential)
        val parameterRequest = PlatformParameterRequest(cloudContext, cloudCredential)
        eventBus!!.notify(parameterRequest.selector(), Event.wrap(parameterRequest))
        try {
            val res = parameterRequest.await()
            LOGGER.info("Platform parameter result: {}", res)
            if (res.status == EventStatus.FAILED) {
                LOGGER.error("Failed to get platform parameters", res.errorDetails)
                throw OperationException(res.errorDetails)
            }
            return res.platformParameters
        } catch (e: InterruptedException) {
            LOGGER.error("Error while getting platform parameters: " + cloudContext, e)
            throw OperationException(e)
        }

    }

    fun checkAndGetPlatformVariant(stack: Stack): Variant {
        LOGGER.debug("Get platform variant for: {}", stack)
        val location = Companion.location(Companion.region(stack.region), Companion.availabilityZone(stack.availabilityZone))
        val cloudContext = CloudContext(stack.id, stack.name, stack.cloudPlatform(), stack.owner, stack.platformVariant,
                location)
        val cloudCredential = credentialConverter!!.convert(stack.credential)
        val checkPlatformVariantRequest = CheckPlatformVariantRequest(cloudContext, cloudCredential)
        eventBus!!.notify(checkPlatformVariantRequest.selector(), Event.wrap(checkPlatformVariantRequest))
        try {
            val res = checkPlatformVariantRequest.await()
            LOGGER.info("Platform variant result: {}", res)
            if (res.status == EventStatus.FAILED) {
                LOGGER.error("Failed to get platform variant", res.errorDetails)
                throw OperationException(res.errorDetails)
            }
            return res.defaultPlatformVariant
        } catch (e: InterruptedException) {
            LOGGER.error("Error while getting the platform variant: " + cloudContext, e)
            throw OperationException(e)
        }

    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(ServiceProviderConnectorAdapter::class.java)
    }

}
