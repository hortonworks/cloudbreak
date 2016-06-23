package com.sequenceiq.cloudbreak.api.endpoint

import javax.validation.Valid
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

import com.sequenceiq.cloudbreak.api.model.IdJson
import com.sequenceiq.cloudbreak.api.model.SubscriptionRequest

@Path("/subscriptions")
@Consumes(MediaType.APPLICATION_JSON)
interface SubscriptionEndpoint {

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    fun subscribe(@Valid subscriptionRequest: SubscriptionRequest): IdJson

}
