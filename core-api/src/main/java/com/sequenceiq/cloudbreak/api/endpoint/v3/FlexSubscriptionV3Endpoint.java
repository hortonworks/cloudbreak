package com.sequenceiq.cloudbreak.api.endpoint.v3;

import static com.sequenceiq.cloudbreak.doc.Notes.FLEX_SUBSCRIPTION_NOTES;

import java.util.Set;

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

import com.sequenceiq.cloudbreak.api.model.FlexSubscriptionRequest;
import com.sequenceiq.cloudbreak.api.model.FlexSubscriptionResponse;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.FlexSubOpDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v3/{workspaceId}/flexsubscriptions")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v3/{workspaceId}/flexsubscriptions", description = ControllerDescription.FLEX_SUBSCRIPTION_V3_DESCRIPTION, protocols = "http,https")
public interface FlexSubscriptionV3Endpoint {

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = FlexSubOpDescription.LIST_BY_WORKSPACE, produces = ContentType.JSON, notes = FLEX_SUBSCRIPTION_NOTES,
            nickname = "listFlexSubscriptionsByWorkspace")
    Set<FlexSubscriptionResponse> listByWorkspace(@PathParam("workspaceId") Long workspaceId);

    @GET
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = FlexSubOpDescription.GET_BY_NAME_IN_WORKSPACE, produces = ContentType.JSON, notes = FLEX_SUBSCRIPTION_NOTES,
            nickname = "getFlexSubscriptionInWorkspace")
    FlexSubscriptionResponse getByNameInWorkspace(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @POST
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = FlexSubOpDescription.CREATE_IN_WORKSPACE, produces = ContentType.JSON, notes = FLEX_SUBSCRIPTION_NOTES,
            nickname = "createFlexSubscriptionInWorkspace")
    FlexSubscriptionResponse createInWorkspace(@PathParam("workspaceId") Long workspaceId, @Valid FlexSubscriptionRequest request);

    @DELETE
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = FlexSubOpDescription.DELETE_BY_NAME_IN_WORKSPACE, produces = ContentType.JSON, notes = FLEX_SUBSCRIPTION_NOTES,
            nickname = "deleteFlexSubscriptionInWorkspace")
    FlexSubscriptionResponse deleteInWorkspace(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @PUT
    @Path("setusedforcontroller/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = FlexSubOpDescription.SET_USED_FOR_CONTROLLER_IN_WORKSPACE, produces = ContentType.JSON, notes = FLEX_SUBSCRIPTION_NOTES,
            nickname = "putUsedForControllerFlexSubscriptionByNameInWorkspace")
    void setUsedForControllerInWorkspace(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @PUT
    @Path("setdefault/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = FlexSubOpDescription.SET_DEFAULT_IN_WORKSPACE, produces = ContentType.JSON, notes = FLEX_SUBSCRIPTION_NOTES,
            nickname = "putDefaultFlexSubscriptionByNameInWorkspace")
    void setDefaultInWorkspace(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

}
