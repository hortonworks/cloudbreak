package com.sequenceiq.cloudbreak.api.endpoint.v4.smartsense;

import static com.sequenceiq.cloudbreak.doc.Notes.SMARTSENSE_SUBSCRIPTION_NOTES;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.api.endpoint.v4.smartsense.responses.SmartSenseSubscriptionV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.smartsense.responses.SmartSenseSubscriptionV4Responses;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.SmartSenseSubOpDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v4/{workspaceId}/smartsense_subscriptions")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v4/{workspaceId}/smartsense_subscriptions", description = ControllerDescription.SMARTSENSE_SUBSCRIPTION_V4_DESCRIPTION,
        protocols = "http,https")
public interface SmartSenseSubscriptionV4Endpoint {

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = SmartSenseSubOpDescription.LIST_BY_WORKSPACE, produces = ContentType.JSON, notes = SMARTSENSE_SUBSCRIPTION_NOTES,
            nickname = "listSmartSenseSubscriptionsByWorkspace")
    SmartSenseSubscriptionV4Responses list(@PathParam("workspaceId") Long workspaceId, @QueryParam("onlyDefault") @DefaultValue("false") Boolean onlyDefault);

    @GET
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = SmartSenseSubOpDescription.GET_BY_NAME_IN_WORKSPACE, produces = ContentType.JSON, notes = SMARTSENSE_SUBSCRIPTION_NOTES,
            nickname = "getSmartSenseSubscriptionInWorkspace")
    SmartSenseSubscriptionV4Response get(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @POST
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = SmartSenseSubOpDescription.CREATE_IN_WORKSPACE, produces = ContentType.JSON, notes = SMARTSENSE_SUBSCRIPTION_NOTES,
            nickname = "createSmartSenseSubscriptionInWorkspace")
    SmartSenseSubscriptionV4Response create(@PathParam("workspaceId") Long workspaceId, @Valid SmartSenseSubscriptionV4Response request);

    @DELETE
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = SmartSenseSubOpDescription.DELETE_BY_NAME_IN_WORKSPACE, produces = ContentType.JSON, notes = SMARTSENSE_SUBSCRIPTION_NOTES,
            nickname = "deleteSmartSenseSubscriptionInWorkspace")
    SmartSenseSubscriptionV4Response delete(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

}
