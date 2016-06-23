package com.sequenceiq.cloudbreak.service.price

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.cloud.model.Platform
import com.sequenceiq.cloudbreak.common.type.CloudConstants
import com.sequenceiq.cloudbreak.domain.Template

@Component
class GCPPriceGenerator : PriceGenerator {

    override fun calculate(template: Template, hours: Long?): Double? {
        return 0.0
    }

    override val cloudPlatform: Platform
        get() = Platform.platform(CloudConstants.GCP)
}
