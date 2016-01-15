package com.sequenceiq.cloudbreak.api.endpoint;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.api.model.IdJson;
import com.sequenceiq.cloudbreak.api.model.SubscriptionRequest;

@Path("/")
@Consumes(MediaType.APPLICATION_JSON)
public interface SubscriptionEndpoint {

    @POST
    @Path("subscriptions")
    @Produces(MediaType.APPLICATION_JSON)
    IdJson subscribe(SubscriptionRequest subscriptionRequest);

}
