package com.sequenceiq.cloudbreak.cloud.arm

import javax.inject.Inject

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
class ArmAuthenticator : Authenticator {

    @Inject
    private val armClient: ArmClient? = null

    override fun platform(): Platform {
        return ArmConstants.AZURE_RM_PLATFORM
    }

    override fun variant(): Variant {
        return ArmConstants.AZURE_RM_VARIANT
    }

    override fun authenticate(cloudContext: CloudContext, cloudCredential: CloudCredential): AuthenticatedContext {
        LOGGER.info("Authenticating to azure ...")
        return armClient!!.createAuthenticatedContext(cloudContext, cloudCredential)
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(ArmAuthenticator::class.java)
    }
}
