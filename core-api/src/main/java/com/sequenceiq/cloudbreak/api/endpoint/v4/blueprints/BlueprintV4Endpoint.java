package com.sequenceiq.cloudbreak.api.endpoint.v4.blueprints;

import static com.sequenceiq.cloudbreak.doc.ContentType.JSON;
import static com.sequenceiq.cloudbreak.doc.Notes.BLUEPRINT_NOTES;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprints.requests.BlueprintV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprints.responses.BlueprintV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprints.responses.BlueprintV4ViewResponses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.ParametersQueryV4Response;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v4/{workspaceId}/blueprints")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v4/{workspaceId}/blueprints", description = ControllerDescription.BLUEPRINT_V4_DESCRIPTION, protocols = "http,https")
public interface BlueprintV4Endpoint {

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.BlueprintOpDescription.LIST_BY_WORKSPACE, produces = JSON, notes = BLUEPRINT_NOTES,
            nickname = "listBlueprintsByWorkspace")
    BlueprintV4ViewResponses list(@PathParam("workspaceId") Long workspaceId);

    @GET
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.BlueprintOpDescription.GET_BY_NAME_IN_WORKSPACE, produces = JSON, notes = BLUEPRINT_NOTES,
            nickname = "getBlueprintInWorkspace")
    BlueprintV4Response get(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @POST
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.BlueprintOpDescription.CREATE_IN_WORKSPACE, produces = JSON, notes = BLUEPRINT_NOTES,
            nickname = "createBlueprintInWorkspace")
    BlueprintV4Response post(@PathParam("workspaceId") Long workspaceId, @Valid BlueprintV4Request request);

    @DELETE
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.BlueprintOpDescription.DELETE_BY_NAME_IN_WORKSPACE, produces = JSON, notes = BLUEPRINT_NOTES,
            nickname = "deleteBlueprintInWorkspace")
    BlueprintV4Response delete(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @GET
    @Path("{name}/request")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.BlueprintOpDescription.GET_BY_BLUEPRINT_NAME, produces = JSON, notes = BLUEPRINT_NOTES,
            nickname = "getBlueprintRequestFromName")
    BlueprintV4Request getRequest(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @GET
    @Path("{name}/parameters")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.UtilityOpDescription.CUSTOM_PARAMETERS, produces = ContentType.JSON,
            nickname = "getBlueprintCustomParameters")
    ParametersQueryV4Response getParameters(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

}
