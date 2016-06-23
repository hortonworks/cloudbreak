package com.sequenceiq.cloudbreak.facade

import javax.inject.Inject

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.convert.ConversionService
import org.springframework.core.convert.TypeDescriptor
import org.springframework.stereotype.Service

import com.sequenceiq.cloudbreak.domain.CloudbreakEvent
import com.sequenceiq.cloudbreak.api.model.CloudbreakEventsJson
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService

@Service
class DefaultCloudbreakEventsFacade : CloudbreakEventsFacade {

    @Inject
    private val cloudbreakEventService: CloudbreakEventService? = null

    @Inject
    @Qualifier("conversionService")
    private val conversionService: ConversionService? = null

    override fun retrieveEvents(owner: String, since: Long?): List<CloudbreakEventsJson> {
        val cloudbreakEvents = cloudbreakEventService!!.cloudbreakEvents(owner, since)
        return conversionService!!.convert(cloudbreakEvents, TypeDescriptor.forObject(cloudbreakEvents), TypeDescriptor.collection(List<Any>::class.java,
                TypeDescriptor.valueOf(CloudbreakEventsJson::class.java))) as List<CloudbreakEventsJson>
    }
}
