package com.sequenceiq.cloudbreak.cloud.handler

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.cloud.CloudConnector
import com.sequenceiq.cloudbreak.cloud.event.platform.CheckPlatformVariantRequest
import com.sequenceiq.cloudbreak.cloud.event.platform.CheckPlatformVariantResult
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors
import com.sequenceiq.cloudbreak.cloud.model.Variant

import reactor.bus.Event

@Component
class CheckPlatformVariantHandler : CloudPlatformEventHandler<CheckPlatformVariantRequest> {

    @Inject
    private val cloudPlatformConnectors: CloudPlatformConnectors? = null

    override fun type(): Class<CheckPlatformVariantRequest> {
        return CheckPlatformVariantRequest::class.java
    }

    override fun accept(defaultPlatformVariantRequestEvent: Event<CheckPlatformVariantRequest>) {
        LOGGER.info("Received event: {}", defaultPlatformVariantRequestEvent)
        val request = defaultPlatformVariantRequestEvent.data
        try {
            val cc = cloudPlatformConnectors!!.get(request.cloudContext.platform, request.cloudContext.variant)
            val defaultVariant = cc.variant()
            val platformParameterResult = CheckPlatformVariantResult(request, defaultVariant)
            request.result.onNext(platformParameterResult)
            LOGGER.info("Query platform variant finished.")
        } catch (e: Exception) {
            request.result.onNext(CheckPlatformVariantResult(e.message, e, request))
        }

    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(CheckPlatformVariantHandler::class.java)
    }
}
