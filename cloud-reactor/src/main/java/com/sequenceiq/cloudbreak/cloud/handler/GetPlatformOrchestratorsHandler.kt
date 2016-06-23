package com.sequenceiq.cloudbreak.cloud.handler

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.google.common.collect.Maps
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformOrchestratorsRequest
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformOrchestratorsResult
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors
import com.sequenceiq.cloudbreak.cloud.model.Orchestrator
import com.sequenceiq.cloudbreak.cloud.model.Platform
import com.sequenceiq.cloudbreak.cloud.model.PlatformOrchestrator
import com.sequenceiq.cloudbreak.cloud.model.PlatformOrchestrators
import com.sequenceiq.cloudbreak.cloud.model.Variant

import reactor.bus.Event

@Component
class GetPlatformOrchestratorsHandler : CloudPlatformEventHandler<GetPlatformOrchestratorsRequest> {

    @Inject
    private val cloudPlatformConnectors: CloudPlatformConnectors? = null

    override fun type(): Class<GetPlatformOrchestratorsRequest> {
        return GetPlatformOrchestratorsRequest::class.java
    }

    override fun accept(getPlatformOrchestratorsRequest: Event<GetPlatformOrchestratorsRequest>) {
        LOGGER.info("Received event: {}", getPlatformOrchestratorsRequest)
        val request = getPlatformOrchestratorsRequest.data
        try {
            val platformCollectionHashMap = Maps.newHashMap<Platform, Collection<Orchestrator>>()
            val defaults = Maps.newHashMap<Platform, Orchestrator>()

            for (connector in cloudPlatformConnectors!!.platformVariants.platformToVariants.entries) {
                val platformOrchestrator = cloudPlatformConnectors.getDefault(connector.key).parameters().orchestratorParams()

                platformCollectionHashMap.put(connector.key, platformOrchestrator.types())
                defaults.put(connector.key, platformOrchestrator.defaultType())
            }
            val getPlatformOrchestratorsResult = GetPlatformOrchestratorsResult(request,
                    PlatformOrchestrators(platformCollectionHashMap, defaults))
            request.result.onNext(getPlatformOrchestratorsResult)
            LOGGER.info("Query platform orchestrators types finished.")
        } catch (e: Exception) {
            request.result.onNext(GetPlatformOrchestratorsResult(e.message, e, request))
        }

    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(GetPlatformOrchestratorsHandler::class.java)
    }
}
