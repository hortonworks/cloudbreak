package com.sequenceiq.cloudbreak.api.endpoint.v3;

import static com.sequenceiq.cloudbreak.doc.ContentType.JSON;
import static com.sequenceiq.cloudbreak.doc.Notes.BLUEPRINT_NOTES;

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

import com.sequenceiq.cloudbreak.api.model.BlueprintRequest;
import com.sequenceiq.cloudbreak.api.model.BlueprintResponse;
import com.sequenceiq.cloudbreak.api.model.BlueprintViewResponse;
import com.sequenceiq.cloudbreak.api.model.ParametersQueryResponse;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.BlueprintOpDescription;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.UtilityOpDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v3/{workspaceId}/blueprints")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v3/{workspaceId}/blueprints", description = ControllerDescription.BLUEPRINT_V3_DESCRIPTION, protocols = "http,https")
public interface BlueprintV3Endpoint {
    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = BlueprintOpDescription.LIST_BY_WORKSPACE, produces = JSON, notes = BLUEPRINT_NOTES,
            nickname = "listBlueprintsByWorkspace")
    Set<BlueprintViewResponse> listByWorkspace(@PathParam("workspaceId") Long workspaceId);

    @GET
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = BlueprintOpDescription.GET_BY_NAME_IN_WORKSPACE, produces = JSON, notes = BLUEPRINT_NOTES,
            nickname = "getBlueprintInWorkspace")
    BlueprintResponse getByNameInWorkspace(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @POST
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = BlueprintOpDescription.CREATE_IN_WORKSPACE, produces = JSON, notes = BLUEPRINT_NOTES,
            nickname = "createBlueprintInWorkspace")
    BlueprintResponse createInWorkspace(@PathParam("workspaceId") Long workspaceId, @Valid BlueprintRequest request);

    @DELETE
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = BlueprintOpDescription.DELETE_BY_NAME_IN_WORKSPACE, produces = JSON, notes = BLUEPRINT_NOTES,
            nickname = "deleteBlueprintInWorkspace")
    BlueprintResponse deleteInWorkspace(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @GET
    @Path("{name}/request")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = BlueprintOpDescription.GET_BY_BLUEPRINT_NAME, produces = JSON, notes = BLUEPRINT_NOTES,
            nickname = "getBlueprintRequestFromName")
    BlueprintRequest getRequestFromName(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @GET
    @Path("{name}/custom-parameters")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = UtilityOpDescription.CUSTOM_PARAMETERS, produces = JSON, nickname = "getBlueprintCustomParameters")
    ParametersQueryResponse getCustomParameters(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

}
