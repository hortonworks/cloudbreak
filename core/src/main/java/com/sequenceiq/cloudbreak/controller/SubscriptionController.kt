package com.sequenceiq.cloudbreak.controller

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.api.endpoint.SubscriptionEndpoint
import com.sequenceiq.cloudbreak.domain.CbUser
import com.sequenceiq.cloudbreak.domain.Subscription
import com.sequenceiq.cloudbreak.logger.MDCBuilder
import com.sequenceiq.cloudbreak.api.model.IdJson
import com.sequenceiq.cloudbreak.api.model.SubscriptionRequest
import com.sequenceiq.cloudbreak.service.subscription.SubscriptionService

@Component
class SubscriptionController : SubscriptionEndpoint {

    @Autowired
    private val subscriptionService: SubscriptionService? = null

    @Autowired
    private val authenticatedUserService: AuthenticatedUserService? = null

    override fun subscribe(subscriptionRequest: SubscriptionRequest): IdJson {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        val subscription = Subscription(SecurityContextHolder.getContext().authentication.principal.toString(),
                subscriptionRequest.endpointUrl)
        return IdJson(subscriptionService!!.subscribe(subscription))
    }
}
