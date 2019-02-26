package com.sequenceiq.cloudbreak.api.endpoint.v3;

import static com.sequenceiq.cloudbreak.doc.Notes.SMARTSENSE_SUBSCRIPTION_NOTES;

import java.util.Set;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
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

@Path("/v3/{workspaceId}/smartsensesubscriptions")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v3/{workspaceId}/smartsensesubscriptions", description = ControllerDescription.SMARTSENSE_SUBSCRIPTION_V3_DESCRIPTION,
        protocols = "http,https")
public interface SmartSenseSubscriptionV3Endpoint {

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = SmartSenseSubOpDescription.LIST_BY_WORKSPACE, produces = ContentType.JSON, notes = SMARTSENSE_SUBSCRIPTION_NOTES,
            nickname = "listSmartSenseSubscriptionsByWorkspace")
    Set<SmartSenseSubscriptionJson> listByWorkspace(@PathParam("workspaceId") Long workspaceId);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = SmartSenseSubOpDescription.GET_DEFAULT_IN_WORKSPACE, produces = ContentType.JSON, notes = SMARTSENSE_SUBSCRIPTION_NOTES,
            nickname = "getDefaultSmartSenseSubscriptionInWorkspace")
    SmartSenseSubscriptionJson getDefaultInWorkspace(@PathParam("workspaceId") Long workspaceId);

    @GET
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = SmartSenseSubOpDescription.GET_BY_NAME_IN_WORKSPACE, produces = ContentType.JSON, notes = SMARTSENSE_SUBSCRIPTION_NOTES,
            nickname = "getSmartSenseSubscriptionInWorkspace")
    SmartSenseSubscriptionJson getByNameInWorkspace(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @POST
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = SmartSenseSubOpDescription.CREATE_IN_WORKSPACE, produces = ContentType.JSON, notes = SMARTSENSE_SUBSCRIPTION_NOTES,
            nickname = "createSmartSenseSubscriptionInWorkspace")
    SmartSenseSubscriptionJson createInWorkspace(@PathParam("workspaceId") Long workspaceId, @Valid SmartSenseSubscriptionJson request);

    @DELETE
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = SmartSenseSubOpDescription.DELETE_BY_NAME_IN_WORKSPACE, produces = ContentType.JSON, notes = SMARTSENSE_SUBSCRIPTION_NOTES,
            nickname = "deleteSmartSenseSubscriptionInWorkspace")
    SmartSenseSubscriptionJson deleteInWorkspace(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

}
