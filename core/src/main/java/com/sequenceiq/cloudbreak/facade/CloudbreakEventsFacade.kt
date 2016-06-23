package com.sequenceiq.cloudbreak.facade

import com.sequenceiq.cloudbreak.api.model.CloudbreakEventsJson

interface CloudbreakEventsFacade {

    fun retrieveEvents(user: String, since: Long?): List<CloudbreakEventsJson>

}
