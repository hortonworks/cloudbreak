package com.sequenceiq.cloudbreak.api.endpoint.v4.recipes;

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

import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.responses.RecipeV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.responses.RecipeV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.responses.RecipeViewV4Responses;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.RecipeOpDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v4/{workspaceId}/recipes")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v4/{workspaceId}/recipes", description = ControllerDescription.RECIPES_V4_DESCRIPTION, protocols = "http,https")
public interface RecipeV4Endpoint {

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = RecipeOpDescription.LIST_BY_WORKSPACE, produces = ContentType.JSON, notes = Notes.RECIPE_NOTES,
            nickname = "listRecipesByWorkspace")
    RecipeViewV4Responses list(@PathParam("workspaceId") Long workspaceId);

    @GET
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = RecipeOpDescription.GET_BY_NAME_IN_WORKSPACE, produces = ContentType.JSON, notes = Notes.RECIPE_NOTES,
            nickname = "getRecipeInWorkspace")
    RecipeV4Response get(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @POST
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = RecipeOpDescription.CREATE_IN_WORKSPACE, produces = ContentType.JSON, notes = Notes.RECIPE_NOTES,
            nickname = "createRecipeInWorkspace")
    RecipeV4Response post(@PathParam("workspaceId") Long workspaceId, @Valid RecipeV4Request request);

    @DELETE
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = RecipeOpDescription.DELETE_BY_NAME_IN_WORKSPACE, produces = ContentType.JSON, notes = Notes.RECIPE_NOTES,
            nickname = "deleteRecipeInWorkspace")
    RecipeV4Response delete(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @DELETE
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = RecipeOpDescription.DELETE_MULTIPLE_BY_NAME_IN_WORKSPACE, produces = ContentType.JSON,
            notes = Notes.RECIPE_NOTES, nickname = "deleteRecipesInWorkspace")
    RecipeV4Responses deleteMultiple(@PathParam("workspaceId") Long workspaceId, Set<String> names);

    @GET
    @Path("{name}/request")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = RecipeOpDescription.GET_REQUEST_BY_NAME, produces = ContentType.JSON, notes = Notes.RECIPE_NOTES,
            nickname = "getRecipeRequestFromNameInWorkspace")
    RecipeV4Request getRequest(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);
}
