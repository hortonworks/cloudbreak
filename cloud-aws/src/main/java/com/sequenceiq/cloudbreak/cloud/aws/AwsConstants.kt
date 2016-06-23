package com.sequenceiq.cloudbreak.cloud.aws

import com.sequenceiq.cloudbreak.common.type.CloudConstants.AWS

import com.sequenceiq.cloudbreak.cloud.model.Platform
import com.sequenceiq.cloudbreak.cloud.model.Variant

object AwsConstants {

    val AWS_PLATFORM = Platform.platform(AWS)
    val AWS_VARIANT = Variant.variant(AWS)
}
