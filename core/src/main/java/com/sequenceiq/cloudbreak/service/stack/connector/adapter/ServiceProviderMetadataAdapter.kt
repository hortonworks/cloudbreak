package com.sequenceiq.cloudbreak.service.stack.connector.adapter

import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone
import com.sequenceiq.cloudbreak.cloud.model.Location.location
import com.sequenceiq.cloudbreak.cloud.model.Region.region
import java.lang.String.format
import java.util.Arrays.asList

import java.util.Collections

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.event.instance.CollectMetadataRequest
import com.sequenceiq.cloudbreak.cloud.event.instance.CollectMetadataResult
import com.sequenceiq.cloudbreak.cloud.event.resource.GetInstancesStateRequest
import com.sequenceiq.cloudbreak.cloud.event.resource.GetInstancesStateResult
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance
import com.sequenceiq.cloudbreak.cloud.model.CloudResource
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus
import com.sequenceiq.cloudbreak.cloud.model.Location
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter
import com.sequenceiq.cloudbreak.converter.spi.InstanceMetaDataToCloudInstanceConverter
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter
import com.sequenceiq.cloudbreak.converter.spi.StackToCloudStackConverter
import com.sequenceiq.cloudbreak.domain.InstanceGroup
import com.sequenceiq.cloudbreak.domain.InstanceMetaData
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.service.stack.connector.OperationException
import com.sequenceiq.cloudbreak.service.stack.flow.InstanceSyncState

import reactor.bus.Event
import reactor.bus.EventBus

@Component
class ServiceProviderMetadataAdapter {
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

    fun collectMetadata(stack: Stack): List<CloudVmMetaDataStatus> {
        val location = Companion.location(Companion.region(stack.region), Companion.availabilityZone(stack.availabilityZone))
        val cloudContext = CloudContext(stack.id, stack.name, stack.cloudPlatform(), stack.owner, stack.platformVariant,
                location)
        val cloudCredential = credentialConverter!!.convert(stack.credential)
        val cloudInstances = cloudStackConverter!!.buildInstances(stack)
        val cloudResources = cloudResourceConverter!!.convert(stack.resources)
        val cmr = CollectMetadataRequest(cloudContext, cloudCredential, cloudResources, cloudInstances)
        LOGGER.info("Triggering event: {}", cmr)
        eventBus!!.notify(cmr.selector(CollectMetadataRequest::class.java), Event.wrap(cmr))
        try {
            val res = cmr.await()
            LOGGER.info("Result: {}", res)
            if (res.errorDetails != null) {
                LOGGER.error("Failed to collect metadata", res.errorDetails)
                return emptyList<CloudVmMetaDataStatus>()
            }
            return res.results
        } catch (e: InterruptedException) {
            LOGGER.error(format("Error while executing collectMetadata, stack: %s", cloudContext), e)
            throw OperationException(e)
        }

    }

    fun getState(stack: Stack, instanceGroup: InstanceGroup, instanceId: String): InstanceSyncState {
        val location = Companion.location(Companion.region(stack.region), Companion.availabilityZone(stack.availabilityZone))
        val cloudContext = CloudContext(stack.id, stack.name, stack.cloudPlatform(), stack.owner, stack.platformVariant,
                location)
        val cloudCredential = credentialConverter!!.convert(stack.credential)
        val ig = stack.getInstanceGroupByInstanceGroupName(instanceGroup.groupName)
        var instance: CloudInstance? = null
        for (metaData in ig.allInstanceMetaData) {
            if (instanceId.equals(metaData.instanceId, ignoreCase = true)) {
                instance = metadataConverter!!.convert(metaData)
                break
            }
        }
        if (instance != null) {
            val stateRequest = GetInstancesStateRequest<GetInstancesStateResult>(cloudContext, cloudCredential, asList<CloudInstance>(instance))
            LOGGER.info("Triggering event: {}", stateRequest)
            eventBus!!.notify(stateRequest.selector(), Event.wrap(stateRequest))
            try {
                val res = stateRequest.await()
                LOGGER.info("Result: {}", res)
                if (res.isFailed) {
                    LOGGER.error("Failed to retrieve instance state", res.errorDetails)
                    throw OperationException(res.errorDetails)
                }
                return transform(res.statuses[0].status)
            } catch (e: InterruptedException) {
                LOGGER.error(format("Error while retrieving instance state of: %s", cloudContext), e)
                throw OperationException(e)
            }

        } else {
            return InstanceSyncState.DELETED
        }
    }

    private fun transform(instanceStatus: InstanceStatus): InstanceSyncState {
        when (instanceStatus) {
            InstanceStatus.IN_PROGRESS -> return InstanceSyncState.IN_PROGRESS
            InstanceStatus.STARTED -> return InstanceSyncState.RUNNING
            InstanceStatus.STOPPED -> return InstanceSyncState.STOPPED
            InstanceStatus.CREATED -> return InstanceSyncState.RUNNING
            InstanceStatus.FAILED -> return InstanceSyncState.DELETED
            InstanceStatus.TERMINATED -> return InstanceSyncState.DELETED
            else -> return InstanceSyncState.UNKNOWN
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(ServiceProviderMetadataAdapter::class.java)
    }

}
