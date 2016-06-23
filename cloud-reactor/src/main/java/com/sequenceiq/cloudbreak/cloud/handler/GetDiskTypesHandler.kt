package com.sequenceiq.cloudbreak.cloud.handler

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.google.common.collect.Maps
import com.sequenceiq.cloudbreak.cloud.event.platform.GetDiskTypesRequest
import com.sequenceiq.cloudbreak.cloud.event.platform.GetDiskTypesResult
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors
import com.sequenceiq.cloudbreak.cloud.model.DiskType
import com.sequenceiq.cloudbreak.cloud.model.DiskTypes
import com.sequenceiq.cloudbreak.cloud.model.Platform
import com.sequenceiq.cloudbreak.cloud.model.PlatformDisks
import com.sequenceiq.cloudbreak.cloud.model.Variant
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterType

import reactor.bus.Event

@Component
class GetDiskTypesHandler : CloudPlatformEventHandler<GetDiskTypesRequest> {

    @Inject
    private val cloudPlatformConnectors: CloudPlatformConnectors? = null

    override fun type(): Class<GetDiskTypesRequest> {
        return GetDiskTypesRequest::class.java
    }

    override fun accept(getDiskTypesRequestEvent: Event<GetDiskTypesRequest>) {
        LOGGER.info("Received event: {}", getDiskTypesRequestEvent)
        val request = getDiskTypesRequestEvent.data
        try {
            val platformDiskTypes = Maps.newHashMap<Platform, Collection<DiskType>>()
            val defaultDiskTypes = Maps.newHashMap<Platform, DiskType>()
            val diskMappings = Maps.newHashMap<Platform, Map<String, VolumeParameterType>>()


            for (connector in cloudPlatformConnectors!!.platformVariants.platformToVariants.entries) {
                val diskTypes = cloudPlatformConnectors.getDefault(connector.key).parameters().diskTypes()
                defaultDiskTypes.put(connector.key, diskTypes.defaultType())
                platformDiskTypes.put(connector.key, diskTypes.types())
                diskMappings.put(connector.key, diskTypes.diskMapping())
            }
            val getDiskTypesResult = GetDiskTypesResult(request, PlatformDisks(platformDiskTypes, defaultDiskTypes, diskMappings))
            request.result.onNext(getDiskTypesResult)
            LOGGER.info("Query platform disk types finished.")
        } catch (e: Exception) {
            request.result.onNext(GetDiskTypesResult(e.message, e, request))
        }

    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(GetDiskTypesHandler::class.java)
    }
}
