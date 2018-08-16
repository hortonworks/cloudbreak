package com.sequenceiq.cloudbreak.api.endpoint.v1;

import static com.sequenceiq.cloudbreak.doc.Notes.SMARTSENSE_SUBSCRIPTION_NOTES;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.api.model.SmartSenseSubscriptionJson;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.SmartSenseSubOpDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v1/smartsensesubscriptions")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v1/smartsensesubscriptions", description = ControllerDescription.SMARTSENSE_SUBSCRIPTION_DESCRIPTION, protocols = "http,https")
public interface SmartSenseSubscriptionEndpoint {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = SmartSenseSubOpDescription.GET, produces = ContentType.JSON, notes = SMARTSENSE_SUBSCRIPTION_NOTES,
            nickname = "getSmartSenseSubscription")
    SmartSenseSubscriptionJson get();

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = SmartSenseSubOpDescription.GET_BY_ID, produces = ContentType.JSON, notes = SMARTSENSE_SUBSCRIPTION_NOTES,
            nickname = "getSmartSenseSubscriptionById")
    SmartSenseSubscriptionJson get(@PathParam("id") Long id);

}
