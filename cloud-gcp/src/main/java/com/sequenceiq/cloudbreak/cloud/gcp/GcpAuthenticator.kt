package com.sequenceiq.cloudbreak.cloud.gcp

import org.springframework.stereotype.Service

import com.sequenceiq.cloudbreak.cloud.Authenticator
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential
import com.sequenceiq.cloudbreak.cloud.model.Platform
import com.sequenceiq.cloudbreak.cloud.model.Variant

@Service
class GcpAuthenticator : Authenticator {

    override fun platform(): Platform {
        return GcpConstants.GCP_PLATFORM
    }

    override fun variant(): Variant {
        return GcpConstants.GCP_VARIANT
    }

    override fun authenticate(cloudContext: CloudContext, cloudCredential: CloudCredential): AuthenticatedContext {
        return AuthenticatedContext(cloudContext, cloudCredential)
    }
}
