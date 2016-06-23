package com.sequenceiq.cloudbreak.cloud.event.setup

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest
import com.sequenceiq.cloudbreak.cloud.context.CloudContext

class SshUserRequest<T>(cloudContext: CloudContext) : CloudPlatformRequest<T>(cloudContext, null)
