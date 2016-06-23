package com.sequenceiq.cloudbreak.cloud.event.setup

import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.event.resource.CloudStackRequest
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential
import com.sequenceiq.cloudbreak.cloud.model.CloudStack

class SetupRequest<T>(cloudContext: CloudContext, cloudCredential: CloudCredential, cloudStack: CloudStack) : CloudStackRequest<T>(cloudContext, cloudCredential, cloudStack)
