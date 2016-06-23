package com.sequenceiq.cloudbreak.facade

import javax.inject.Inject
import javax.transaction.Transactional

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.convert.ConversionService
import org.springframework.core.convert.TypeDescriptor
import org.springframework.stereotype.Service

import com.sequenceiq.cloudbreak.domain.CbUsageFilterParameters
import com.sequenceiq.cloudbreak.domain.CloudbreakUsage
import com.sequenceiq.cloudbreak.api.model.CloudbreakUsageJson
import com.sequenceiq.cloudbreak.service.usages.CloudbreakUsageGeneratorService
import com.sequenceiq.cloudbreak.service.usages.CloudbreakUsagesRetrievalService

@Service
@Transactional
class DefaultCloudbreakUsagesFacade : CloudbreakUsagesFacade {

    @Inject
    private val cloudbreakUsagesService: CloudbreakUsagesRetrievalService? = null

    @Inject
    private val cloudbreakUsageGeneratorService: CloudbreakUsageGeneratorService? = null

    @Inject
    @Qualifier("conversionService")
    private val conversionService: ConversionService? = null

    override fun getUsagesFor(params: CbUsageFilterParameters): List<CloudbreakUsageJson> {
        val usages = cloudbreakUsagesService!!.findUsagesFor(params)
        return conversionService!!.convert(usages, TypeDescriptor.forObject(usages), TypeDescriptor.collection(List<Any>::class.java,
                TypeDescriptor.valueOf(CloudbreakUsageJson::class.java))) as List<CloudbreakUsageJson>
    }

    override fun generateUserUsages() {
        cloudbreakUsageGeneratorService!!.generate()
    }
}
