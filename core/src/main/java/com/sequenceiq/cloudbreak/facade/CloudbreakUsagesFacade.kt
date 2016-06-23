package com.sequenceiq.cloudbreak.facade

import com.sequenceiq.cloudbreak.domain.CbUsageFilterParameters
import com.sequenceiq.cloudbreak.api.model.CloudbreakUsageJson

interface CloudbreakUsagesFacade {

    fun getUsagesFor(params: CbUsageFilterParameters): List<CloudbreakUsageJson>

    fun generateUserUsages()

}
