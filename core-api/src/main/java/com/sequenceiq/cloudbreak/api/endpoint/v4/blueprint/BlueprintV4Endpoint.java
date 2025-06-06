package com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint;

import static com.sequenceiq.cloudbreak.doc.Notes.BLUEPRINT_NOTES;

import java.util.Set;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.requests.BlueprintV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses.BlueprintV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses.BlueprintV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses.BlueprintV4ViewResponses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.ParametersQueryV4Response;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.BlueprintOpDescription;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.UtilityOpDescription;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RetryAndMetrics
@Path("/v4/{workspaceId}/blueprints")
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "/v4/{workspaceId}/blueprints", description = ControllerDescription.BLUEPRINT_V4_DESCRIPTION)
public interface BlueprintV4Endpoint {

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = BlueprintOpDescription.LIST_BY_WORKSPACE, description = BLUEPRINT_NOTES,
            operationId = "listBlueprintsByWorkspace",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    BlueprintV4ViewResponses list(@PathParam("workspaceId") Long workspaceId, @DefaultValue("false") @QueryParam("withSdx") Boolean withSdx);

    @GET
    @Path("name/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = BlueprintOpDescription.GET_BY_NAME_IN_WORKSPACE, description = BLUEPRINT_NOTES,
            operationId = "getBlueprintInWorkspace",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    BlueprintV4Response getByName(@PathParam("workspaceId") Long workspaceId, @PathParam("name") @NotNull String name);

    @GET
    @Path("name/{name}/internal")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = BlueprintOpDescription.GET_BY_NAME_IN_WORKSPACE_INTERNAL, description = BLUEPRINT_NOTES,
            operationId = "getBlueprintInWorkspaceInternal",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    BlueprintV4Response getByNameInternal(@PathParam("workspaceId") Long workspaceId, @QueryParam("accountId") String accountId,
            @PathParam("name") @NotNull String name);

    @GET
    @Path("crn/{crn}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = BlueprintOpDescription.GET_BY_CRN_IN_WORKSPACE, description = BLUEPRINT_NOTES,
            operationId = "getBlueprintByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    BlueprintV4Response getByCrn(@PathParam("workspaceId") Long workspaceId, @PathParam("crn") @NotNull String crn);

    @POST
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = BlueprintOpDescription.CREATE_IN_WORKSPACE, description = BLUEPRINT_NOTES,
            operationId = "createBlueprintInWorkspace",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    BlueprintV4Response post(@PathParam("workspaceId") Long workspaceId, @Valid BlueprintV4Request request);

    @POST
    @Path("internal")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = BlueprintOpDescription.CREATE_IN_WORKSPACE_INTERNAL, description = BLUEPRINT_NOTES,
            operationId = "createBlueprintInWorkspaceInternal",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    BlueprintV4Response postInternal(@QueryParam("accountId") String accountId,  @PathParam("workspaceId") Long workspaceId,
            @Valid BlueprintV4Request request);

    @DELETE
    @Path("name/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = BlueprintOpDescription.DELETE_BY_NAME_IN_WORKSPACE, description = BLUEPRINT_NOTES,
            operationId = "deleteBlueprintInWorkspace",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    BlueprintV4Response deleteByName(@PathParam("workspaceId") Long workspaceId, @PathParam("name") @NotNull String name);

    @DELETE
    @Path("crn/{crn}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = BlueprintOpDescription.DELETE_BY_CRN_IN_WORKSPACE, description = BLUEPRINT_NOTES,
            operationId = "deleteBlueprintByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    BlueprintV4Response deleteByCrn(@PathParam("workspaceId") Long workspaceId, @PathParam("crn") @NotNull String crn);

    @DELETE
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = BlueprintOpDescription.DELETE_MULTIPLE_BY_NAME_IN_WORKSPACE,
            description = BLUEPRINT_NOTES, operationId = "deleteBlueprintsInWorkspace",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    BlueprintV4Responses deleteMultiple(@PathParam("workspaceId") Long workspaceId, Set<String> names);

    @GET
    @Path("{name}/request")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = BlueprintOpDescription.GET_BY_NAME, description = BLUEPRINT_NOTES,
            operationId = "getBlueprintRequestFromName",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    BlueprintV4Request getRequest(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @GET
    @Path("{name}/parameters")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = UtilityOpDescription.CUSTOM_PARAMETERS,
            operationId = "getBlueprintCustomParameters",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    ParametersQueryV4Response getParameters(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);
}
