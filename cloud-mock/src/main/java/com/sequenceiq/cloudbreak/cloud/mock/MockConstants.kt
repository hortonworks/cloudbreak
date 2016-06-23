package com.sequenceiq.cloudbreak.cloud.mock

import com.sequenceiq.cloudbreak.cloud.model.Platform
import com.sequenceiq.cloudbreak.cloud.model.Variant

/**
 * Created by perdos on 4/22/16.
 */
object MockConstants {

    val MOCK = "MOCK"
    val MOCK_PLATFORM = Platform.platform(MOCK)
    val MOCK_VARIANT = Variant.variant(MOCK)
}
