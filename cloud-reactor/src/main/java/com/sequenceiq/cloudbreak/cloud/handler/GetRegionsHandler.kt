package com.sequenceiq.cloudbreak.cloud.handler

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.google.common.collect.Maps
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformRegionsRequest
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformRegionsResult
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone
import com.sequenceiq.cloudbreak.cloud.model.Platform
import com.sequenceiq.cloudbreak.cloud.model.PlatformRegions
import com.sequenceiq.cloudbreak.cloud.model.Region
import com.sequenceiq.cloudbreak.cloud.model.Variant

import reactor.bus.Event

@Component
class GetRegionsHandler : CloudPlatformEventHandler<GetPlatformRegionsRequest> {

    @Inject
    private val cloudPlatformConnectors: CloudPlatformConnectors? = null

    override fun type(): Class<GetPlatformRegionsRequest> {
        return GetPlatformRegionsRequest::class.java
    }

    override fun accept(getRegionsRequestEvent: Event<GetPlatformRegionsRequest>) {
        LOGGER.info("Received event: {}", getRegionsRequestEvent)
        val request = getRegionsRequestEvent.data
        try {
            val platformRegions = Maps.newHashMap<Platform, Collection<Region>>()
            val platformAvailabilityZones = Maps.newHashMap<Platform, Map<Region, List<AvailabilityZone>>>()
            val platformDefaultRegion = Maps.newHashMap<Platform, Region>()
            for (connector in cloudPlatformConnectors!!.platformVariants.platformToVariants.entries) {
                val defaultRegion = cloudPlatformConnectors.getDefault(connector.key).parameters().regions().defaultType()
                val regions = cloudPlatformConnectors.getDefault(connector.key).parameters().regions().types()
                val availabilityZones = cloudPlatformConnectors.getDefault(connector.key).parameters().availabilityZones().all
                platformAvailabilityZones.put(connector.key, availabilityZones)
                platformRegions.put(connector.key, regions)
                platformDefaultRegion.put(connector.key, defaultRegion)
            }
            val pv = PlatformRegions(platformRegions, platformAvailabilityZones, platformDefaultRegion)
            val getPlatformRegionsResult = GetPlatformRegionsResult(request, pv)
            request.result.onNext(getPlatformRegionsResult)
            LOGGER.info("Query platform machine types types finished.")
        } catch (e: Exception) {
            request.result.onNext(GetPlatformRegionsResult(e.message, e, request))
        }

    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(GetRegionsHandler::class.java)
    }
}
