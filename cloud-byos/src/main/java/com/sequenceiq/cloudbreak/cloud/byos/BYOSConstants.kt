package com.sequenceiq.cloudbreak.cloud.byos

import com.sequenceiq.cloudbreak.common.type.CloudConstants.BYOS

import com.sequenceiq.cloudbreak.cloud.model.Platform
import com.sequenceiq.cloudbreak.cloud.model.Variant

object BYOSConstants {

    val BYOS_PLATFORM = Platform.platform(BYOS)
    val BYOS_VARIANT = Variant.variant(BYOS)
}