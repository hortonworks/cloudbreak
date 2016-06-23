package com.sequenceiq.cloudbreak.service.usages

import javax.inject.Inject

import org.springframework.data.jpa.domain.Specifications
import org.springframework.stereotype.Service

import com.sequenceiq.cloudbreak.domain.CbUsageFilterParameters
import com.sequenceiq.cloudbreak.domain.CloudbreakUsage
import com.sequenceiq.cloudbreak.repository.CloudbreakUsageRepository
import com.sequenceiq.cloudbreak.repository.CloudbreakUsageSpecifications

@Service
class DefaultCloudbreakUsagesRetrievalService : CloudbreakUsagesRetrievalService {

    @Inject
    private val usageRepository: CloudbreakUsageRepository? = null

    override fun findUsagesFor(params: CbUsageFilterParameters): List<CloudbreakUsage> {
        val usages = usageRepository!!.findAll(
                Specifications.where(CloudbreakUsageSpecifications.usagesWithStringFields("account", params.account)).and(CloudbreakUsageSpecifications.usagesWithStringFields("owner", params.owner)).and(CloudbreakUsageSpecifications.usagesSince(params.since)).and(CloudbreakUsageSpecifications.usagesBefore(params.filterEndDate)).and(CloudbreakUsageSpecifications.usagesWithStringFields("provider", params.cloud)).and(CloudbreakUsageSpecifications.usagesWithStringFields("region", params.region)))
        return usages
    }
}
