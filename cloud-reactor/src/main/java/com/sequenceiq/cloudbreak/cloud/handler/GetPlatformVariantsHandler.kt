package com.sequenceiq.cloudbreak.cloud.handler

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformVariantsRequest
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformVariantsResult
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors
import com.sequenceiq.cloudbreak.cloud.model.PlatformVariants

import reactor.bus.Event

@Component
class GetPlatformVariantsHandler : CloudPlatformEventHandler<GetPlatformVariantsRequest> {

    @Inject
    private val cloudPlatformConnectors: CloudPlatformConnectors? = null

    override fun type(): Class<GetPlatformVariantsRequest> {
        return GetPlatformVariantsRequest::class.java
    }

    override fun accept(getPlatformVariantsRequestEvent: Event<GetPlatformVariantsRequest>) {
        LOGGER.info("Received event: {}", getPlatformVariantsRequestEvent)
        val request = getPlatformVariantsRequestEvent.data
        try {
            val pv = cloudPlatformConnectors!!.platformVariants
            val platformVariantResult = GetPlatformVariantsResult(request, pv)
            request.result.onNext(platformVariantResult)
            LOGGER.info("Query platform variant finished.")
        } catch (e: Exception) {
            request.result.onNext(GetPlatformVariantsResult(e.message, e, request))
        }

    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(GetPlatformVariantsHandler::class.java)
    }
}
