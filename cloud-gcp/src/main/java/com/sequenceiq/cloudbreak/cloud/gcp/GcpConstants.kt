package com.sequenceiq.cloudbreak.cloud.gcp

import com.sequenceiq.cloudbreak.common.type.CloudConstants.GCP

import com.sequenceiq.cloudbreak.cloud.model.Platform
import com.sequenceiq.cloudbreak.cloud.model.Variant

object GcpConstants {

    val GCP_PLATFORM = Platform.platform(GCP)
    val GCP_VARIANT = Variant.variant(GCP)
}
