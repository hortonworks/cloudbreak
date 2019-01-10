package com.sequenceiq.cloudbreak.api.endpoint.v4.flexsubscriptions;

import static com.sequenceiq.cloudbreak.doc.Notes.FLEX_SUBSCRIPTION_NOTES;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.responses.GeneralSetV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.flexsubscriptions.requests.FlexSubscriptionV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.flexsubscriptions.responses.FlexSubscriptionV4Response;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.FlexSubOpDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v4/{workspaceId}/flex_subscriptions")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v4/{workspaceId}/flex_subscriptions", description = ControllerDescription.FLEX_SUBSCRIPTION_V4_DESCRIPTION, protocols = "http,https")
public interface FlexSubscriptionV4Endpoint {

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = FlexSubOpDescription.LIST_BY_WORKSPACE, produces = ContentType.JSON, notes = Notes.FLEX_SUBSCRIPTION_NOTES,
            nickname = "listFlexSubscriptionsByWorkspace")
    GeneralSetV4Response<FlexSubscriptionV4Response> list(@PathParam("workspaceId") Long workspaceId);

    @GET
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = FlexSubOpDescription.GET_BY_NAME_IN_WORKSPACE, produces = ContentType.JSON, notes = Notes.FLEX_SUBSCRIPTION_NOTES,
            nickname = "getFlexSubscriptionInWorkspace")
    FlexSubscriptionV4Response get(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @POST
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = FlexSubOpDescription.CREATE_IN_WORKSPACE, produces = ContentType.JSON, notes = Notes.FLEX_SUBSCRIPTION_NOTES,
            nickname = "createFlexSubscriptionInWorkspace")
    FlexSubscriptionV4Response create(@PathParam("workspaceId") Long workspaceId, @Valid FlexSubscriptionV4Request request);

    @DELETE
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = FlexSubOpDescription.DELETE_BY_NAME_IN_WORKSPACE, produces = ContentType.JSON, notes = Notes.FLEX_SUBSCRIPTION_NOTES,
            nickname = "deleteFlexSubscriptionInWorkspace")
    FlexSubscriptionV4Response delete(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @PUT
    @Path("{name}/set_for_controller")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = FlexSubOpDescription.SET_USED_FOR_CONTROLLER_IN_WORKSPACE, produces = ContentType.JSON, notes = FLEX_SUBSCRIPTION_NOTES,
            nickname = "putUsedForControllerFlexSubscriptionByNameInWorkspace")
    FlexSubscriptionV4Response setUsedForController(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @PUT
    @Path("{name}/default")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = FlexSubOpDescription.SET_DEFAULT_IN_WORKSPACE, produces = ContentType.JSON, notes = FLEX_SUBSCRIPTION_NOTES,
            nickname = "putDefaultFlexSubscriptionByNameInWorkspace")
    FlexSubscriptionV4Response setDefault(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

}
