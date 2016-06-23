package com.sequenceiq.cloudbreak.cloud.event.setup

import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential
import com.sequenceiq.cloudbreak.cloud.model.CloudStack
import com.sequenceiq.cloudbreak.cloud.model.Image

class PrepareImageRequest<T>(cloudContext: CloudContext, cloudCredential: CloudCredential, val stack: CloudStack, val image: Image) : CloudPlatformRequest<PrepareImageResult>(cloudContext, cloudCredential)
