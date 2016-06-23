package com.sequenceiq.cloudbreak.service.price

import com.sequenceiq.cloudbreak.cloud.model.Platform
import com.sequenceiq.cloudbreak.domain.Template

interface PriceGenerator {

    fun calculate(template: Template, hours: Long?): Double?

    val cloudPlatform: Platform

}
