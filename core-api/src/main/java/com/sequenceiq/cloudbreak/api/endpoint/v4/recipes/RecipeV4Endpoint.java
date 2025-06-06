package com.sequenceiq.cloudbreak.api.endpoint.v4.recipes;

import java.util.Set;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.responses.RecipeV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.responses.RecipeV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.responses.RecipeViewV4Responses;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.RecipeOpDescription;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RetryAndMetrics
@Path("/v4/{workspaceId}/recipes")
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "/v4/{workspaceId}/recipes", description = ControllerDescription.RECIPES_V4_DESCRIPTION)
public interface RecipeV4Endpoint {

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = RecipeOpDescription.LIST_BY_WORKSPACE, description = Notes.RECIPE_NOTES,
            operationId = "listRecipesByWorkspace",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    RecipeViewV4Responses list(@PathParam("workspaceId") Long workspaceId);

    @GET
    @Path("internal")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = RecipeOpDescription.LIST_BY_WORKSPACE_INTERNAL, description = Notes.RECIPE_NOTES,
            operationId = "listRecipesByWorkspaceInternal",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    RecipeViewV4Responses listInternal(@PathParam("workspaceId") Long workspaceId, @QueryParam("initiatorUserCrn") String initiatorUserCrn);

    @GET
    @Path("name/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = RecipeOpDescription.GET_BY_NAME_IN_WORKSPACE, description = Notes.RECIPE_NOTES,
            operationId = "getRecipeInWorkspace",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    RecipeV4Response getByName(@PathParam("workspaceId") Long workspaceId, @PathParam("name") @NotNull String name);

    @GET
    @Path("name/{name}/internal")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = RecipeOpDescription.GET_BY_NAME_IN_WORKSPACE_INTERNAL, description = Notes.RECIPE_NOTES,
            operationId = "getRecipeInWorkspaceInternal",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    RecipeV4Response getByNameInternal(@PathParam("workspaceId") Long workspaceId, @QueryParam("accountId") String accountId,
            @PathParam("name") @NotNull String name);

    @GET
    @Path("crn/{crn}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = RecipeOpDescription.GET_BY_CRN_IN_WORKSPACE, description = Notes.RECIPE_NOTES,
            operationId = "getRecipeByCrnInWorkspace",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    RecipeV4Response getByCrn(@PathParam("workspaceId") Long workspaceId, @PathParam("crn") @NotNull String crn);

    @POST
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = RecipeOpDescription.CREATE_IN_WORKSPACE, description = Notes.RECIPE_NOTES,
            operationId = "createRecipeInWorkspace",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    RecipeV4Response post(@PathParam("workspaceId") Long workspaceId, @Valid RecipeV4Request request);

    @POST
    @Path("internal")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = RecipeOpDescription.CREATE_IN_WORKSPACE_INTERNAL, description = Notes.RECIPE_NOTES,
            operationId = "createRecipeInWorkspaceInternal",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    RecipeV4Response postInternal(@QueryParam("accountId") String accountId,
            @PathParam("workspaceId") Long workspaceId, @Valid RecipeV4Request request);

    @DELETE
    @Path("name/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = RecipeOpDescription.DELETE_BY_NAME_IN_WORKSPACE, description = Notes.RECIPE_NOTES,
            operationId = "deleteRecipeInWorkspace",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    RecipeV4Response deleteByName(@PathParam("workspaceId") Long workspaceId, @PathParam("name") @NotNull String name);

    @DELETE
    @Path("crn/{crn}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = RecipeOpDescription.DELETE_BY_CRN_IN_WORKSPACE, description = Notes.RECIPE_NOTES,
            operationId = "deleteRecipeByCrnInWorkspace",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    RecipeV4Response deleteByCrn(@PathParam("workspaceId") Long workspaceId, @PathParam("crn") @NotNull String crn);

    @DELETE
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = RecipeOpDescription.DELETE_MULTIPLE_BY_NAME_IN_WORKSPACE,
            description = Notes.RECIPE_NOTES, operationId = "deleteRecipesInWorkspace",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    RecipeV4Responses deleteMultiple(@PathParam("workspaceId") Long workspaceId, Set<String> names);

    @GET
    @Path("{name}/request")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = RecipeOpDescription.GET_REQUEST_BY_NAME, description = Notes.RECIPE_NOTES,
            operationId = "getRecipeRequestFromNameInWorkspace",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    RecipeV4Request getRequest(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @GET
    @Path("names")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = RecipeOpDescription.GET_REQUESTS_BY_NAMES, description = Notes.RECIPE_NOTES,
            operationId = "getRecipeRequestsFromNamesInWorkspace",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    Set<RecipeV4Request> getRequestsByNames(@PathParam("workspaceId") Long workspaceId, @NotNull @QueryParam("names") Set<String> names,
            @QueryParam("initiatorUserCrn") String initiatorUserCrn);
}
