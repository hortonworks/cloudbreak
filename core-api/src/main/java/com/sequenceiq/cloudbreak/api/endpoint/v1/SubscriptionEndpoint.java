package com.sequenceiq.cloudbreak.api.endpoint.v1;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.api.model.IdJson;
import com.sequenceiq.cloudbreak.api.model.SubscriptionRequest;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.SubscriptionOpDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v1/subscriptions")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v1/subscriptions", description = ControllerDescription.SUBSCRIPTION_DESCRIPTION, protocols = "http,https")
public interface SubscriptionEndpoint {

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = SubscriptionOpDescription.SUBSCRIBE, produces = ContentType.JSON, notes = Notes.SUBSCRIPTION_NOTES,
            nickname = "subscribeSubscription")
    IdJson subscribe(@Valid SubscriptionRequest subscriptionRequest);

}
