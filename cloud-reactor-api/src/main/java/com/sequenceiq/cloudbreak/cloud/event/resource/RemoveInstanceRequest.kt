package com.sequenceiq.cloudbreak.cloud.event.resource

import java.util.Collections

import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance
import com.sequenceiq.cloudbreak.cloud.model.CloudResource
import com.sequenceiq.cloudbreak.cloud.model.CloudStack

class RemoveInstanceRequest<T>(cloudContext: CloudContext, cloudCredential: CloudCredential, cloudStack: CloudStack, cloudResources: List<CloudResource>,
                               instance: CloudInstance) : DownscaleStackRequest<T>(cloudContext, cloudCredential, cloudStack, cloudResources, listOf<CloudInstance>(instance))
