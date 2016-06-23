package com.sequenceiq.cloudbreak.cloud.event.resource

import com.sequenceiq.cloudbreak.api.model.AdjustmentType
import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential
import com.sequenceiq.cloudbreak.cloud.model.CloudStack

class LaunchStackRequest(cloudCtx: CloudContext, cloudCredential: CloudCredential, cloudStack: CloudStack, val adjustmentType: AdjustmentType, val threshold: Long?) : CloudStackRequest<LaunchStackResult>(cloudCtx, cloudCredential, cloudStack)
