package com.sequenceiq.cloudbreak.controller

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.api.endpoint.EventEndpoint
import com.sequenceiq.cloudbreak.domain.CbUser
import com.sequenceiq.cloudbreak.facade.CloudbreakEventsFacade
import com.sequenceiq.cloudbreak.logger.MDCBuilder
import com.sequenceiq.cloudbreak.api.model.CloudbreakEventsJson

@Component
class CloudbreakEventController : EventEndpoint {

    @Autowired
    private val cloudbreakEventsFacade: CloudbreakEventsFacade? = null

    @Autowired
    private val authenticatedUserService: AuthenticatedUserService? = null

    override fun get(since: Long?): List<CloudbreakEventsJson> {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        val cloudbreakEvents = cloudbreakEventsFacade!!.retrieveEvents(user.userId, since)
        return cloudbreakEvents
    }
}
