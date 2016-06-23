package com.sequenceiq.cloudbreak.service.usages

import com.sequenceiq.cloudbreak.domain.CbUsageFilterParameters
import com.sequenceiq.cloudbreak.domain.CloudbreakUsage

interface CloudbreakUsagesRetrievalService {
    fun findUsagesFor(params: CbUsageFilterParameters): List<CloudbreakUsage>
}
