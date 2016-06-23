package com.sequenceiq.cloudbreak.cloud.event.resource

import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance
import com.sequenceiq.cloudbreak.cloud.model.CloudResource
import com.sequenceiq.cloudbreak.cloud.model.CloudStack

open class DownscaleStackRequest<T>(cloudContext: CloudContext, cloudCredential: CloudCredential, cloudStack: CloudStack, val cloudResources: List<CloudResource>,
                                    val instances: List<CloudInstance>) : CloudStackRequest<T>(cloudContext, cloudCredential, cloudStack)
