package com.sequenceiq.cloudbreak.service.stack

import com.sequenceiq.cloudbreak.api.model.StackRequest
import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus
import com.sequenceiq.cloudbreak.cloud.event.platform.GetStackParamValidationRequest
import com.sequenceiq.cloudbreak.cloud.event.platform.GetStackParamValidationResult
import com.sequenceiq.cloudbreak.cloud.model.StackParamValidation
import com.sequenceiq.cloudbreak.domain.Credential
import com.sequenceiq.cloudbreak.service.credential.CredentialService
import com.sequenceiq.cloudbreak.service.stack.connector.OperationException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.bus.Event
import reactor.bus.EventBus

import javax.inject.Inject
import java.util.Collections

@Service
class StackParameterService {

    @Inject
    private val credentialService: CredentialService? = null

    @Inject
    private val eventBus: EventBus? = null

    fun getStackParams(stackRequest: StackRequest): List<StackParamValidation> {
        LOGGER.debug("Get stack params")
        val credentialId = stackRequest.credentialId
        if (credentialId != null) {
            val credential = credentialService!!.get(credentialId)
            val cloudContext = CloudContext(credential.id, stackRequest.name, credential.cloudPlatform(), credential.owner)

            val getStackParamValidationRequest = GetStackParamValidationRequest(cloudContext)
            eventBus!!.notify(getStackParamValidationRequest.selector(), Event.wrap(getStackParamValidationRequest))
            try {
                val res = getStackParamValidationRequest.await()
                LOGGER.info("Get stack params result: {}", res)
                if (res.status == EventStatus.FAILED) {
                    LOGGER.error("Failed to get stack params", res.errorDetails)
                    throw OperationException(res.errorDetails)
                }
                return res.stackParamValidations
            } catch (e: InterruptedException) {
                LOGGER.error("Error while getting the stack params", e)
                throw OperationException(e)
            }

        } else {
            return emptyList<StackParamValidation>()
        }
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(StackParameterService::class.java)
    }
}
