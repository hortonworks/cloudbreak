package com.sequenceiq.cloudbreak.cloud.arm

import com.sequenceiq.cloudbreak.common.type.CloudConstants.AZURE_RM

import com.sequenceiq.cloudbreak.cloud.model.Platform
import com.sequenceiq.cloudbreak.cloud.model.Variant

object ArmConstants {

    val AZURE_RM_PLATFORM = Platform.platform(AZURE_RM)
    val AZURE_RM_VARIANT = Variant.variant(AZURE_RM)
}
