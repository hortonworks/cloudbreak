package com.sequenceiq.cloudbreak.cloud.handler

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.cloud.CloudConnector
import com.sequenceiq.cloudbreak.cloud.event.platform.GetStackParamValidationRequest
import com.sequenceiq.cloudbreak.cloud.event.platform.GetStackParamValidationResult
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors

import reactor.bus.Event

@Component
class GetStackParamValidationHandler : CloudPlatformEventHandler<GetStackParamValidationRequest> {

    @Inject
    private val cloudPlatformConnectors: CloudPlatformConnectors? = null

    override fun type(): Class<GetStackParamValidationRequest> {
        return GetStackParamValidationRequest::class.java
    }

    override fun accept(getStackParametersRequestEvent: Event<GetStackParamValidationRequest>) {
        LOGGER.info("Received event: {}", getStackParametersRequestEvent)
        val request = getStackParametersRequestEvent.data
        try {
            val aDefault = cloudPlatformConnectors!!.getDefault(request.cloudContext.platform)
            val getStackParamValidationResult = GetStackParamValidationResult(request,
                    aDefault.parameters().additionalStackParameters())
            request.result.onNext(getStackParamValidationResult)
            LOGGER.info("Query platform stack parameters finished.")
        } catch (e: Exception) {
            request.result.onNext(GetStackParamValidationResult(e.message, e, request))
        }

    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(GetStackParamValidationHandler::class.java)
    }
}
