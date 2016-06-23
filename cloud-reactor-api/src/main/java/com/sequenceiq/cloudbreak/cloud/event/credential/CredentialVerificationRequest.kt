package com.sequenceiq.cloudbreak.cloud.event.credential

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest
import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential

class CredentialVerificationRequest(cloudContext: CloudContext, cloudCredential: CloudCredential) : CloudPlatformRequest<CredentialVerificationResult>(cloudContext, cloudCredential)
