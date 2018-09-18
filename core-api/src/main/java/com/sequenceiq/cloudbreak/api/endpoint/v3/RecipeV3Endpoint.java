package com.sequenceiq.cloudbreak.api.endpoint.v3;

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

import com.sequenceiq.cloudbreak.api.model.RecipeRequest;
import com.sequenceiq.cloudbreak.api.model.RecipeResponse;
import com.sequenceiq.cloudbreak.api.model.RecipeViewResponse;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.RecipeOpDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v3/{workspaceId}/recipes")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v3/{workspaceId}/recipes", description = ControllerDescription.RECIPE_V3_DESCRIPTION, protocols = "http,https")
public interface RecipeV3Endpoint {

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = RecipeOpDescription.LIST_BY_WORKSPACE, produces = ContentType.JSON, notes = Notes.RECIPE_NOTES,
            nickname = "listRecipesByWorkspace")
    Set<RecipeViewResponse> listByWorkspace(@PathParam("workspaceId") Long workspaceId);

    @GET
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = RecipeOpDescription.GET_BY_NAME_IN_WORKSPACE, produces = ContentType.JSON, notes = Notes.RECIPE_NOTES,
            nickname = "getRecipeInWorkspace")
    RecipeResponse getByNameInWorkspace(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @POST
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = RecipeOpDescription.CREATE_IN_WORKSPACE, produces = ContentType.JSON, notes = Notes.RECIPE_NOTES,
            nickname = "createRecipeInWorkspace")
    RecipeResponse createInWorkspace(@PathParam("workspaceId") Long workspaceId, @Valid RecipeRequest request);

    @DELETE
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = RecipeOpDescription.DELETE_BY_NAME_IN_WORKSPACE, produces = ContentType.JSON, notes = Notes.RECIPE_NOTES,
            nickname = "deleteRecipeInWorkspace")
    RecipeResponse deleteInWorkspace(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @GET
    @Path("{name}/request")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = RecipeOpDescription.GET_REQUEST_BY_NAME, produces = ContentType.JSON, notes = Notes.RECIPE_NOTES,
            nickname = "getRecipeRequestFromNameInWorkspace")
    RecipeRequest getRequestFromName(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);
}
