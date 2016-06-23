package com.sequenceiq.cloudbreak.cloud.mock

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

import com.sequenceiq.cloudbreak.cloud.Authenticator
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential
import com.sequenceiq.cloudbreak.cloud.model.Platform
import com.sequenceiq.cloudbreak.cloud.model.Variant

@Service
class MockAuthenticator : Authenticator {

    override fun authenticate(cloudContext: CloudContext, cloudCredential: CloudCredential): AuthenticatedContext {
        LOGGER.info("Authenticating to mock ...")
        return AuthenticatedContext(cloudContext, cloudCredential)
    }

    override fun platform(): Platform {
        return MockConstants.MOCK_PLATFORM
    }

    override fun variant(): Variant {
        return MockConstants.MOCK_VARIANT
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(MockAuthenticator::class.java)
    }
}