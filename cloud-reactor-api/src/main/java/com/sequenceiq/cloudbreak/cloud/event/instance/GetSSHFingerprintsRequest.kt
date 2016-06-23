package com.sequenceiq.cloudbreak.cloud.event.instance

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest
import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance

class GetSSHFingerprintsRequest<T>(cloudContext: CloudContext, cloudCredential: CloudCredential, val cloudInstance: CloudInstance) : CloudPlatformRequest<T>(cloudContext, cloudCredential)
