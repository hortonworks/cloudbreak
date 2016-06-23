package com.sequenceiq.cloudbreak.cloud.openstack.auth

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
import com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackConstants

@Service
class OpenStackAuthenticator : Authenticator {

    @Inject
    private val openStackClient: OpenStackClient? = null

    override fun platform(): Platform {
        return OpenStackConstants.OPENSTACK_PLATFORM
    }

    override fun variant(): Variant {
        return Variant.EMPTY
    }

    override fun authenticate(cloudContext: CloudContext, cloudCredential: CloudCredential): AuthenticatedContext {
        LOGGER.info("Authenticating to openstack ...")
        return openStackClient!!.createAuthenticatedContext(cloudContext, cloudCredential)
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(OpenStackAuthenticator::class.java)
    }
}
