package com.sequenceiq.cloudbreak.api.endpoint.v1;

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
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.RecipeOpDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v1/recipes")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v1/recipes", description = ControllerDescription.RECIPE_DESCRIPTION, protocols = "http,https")
public interface RecipeEndpoint {

    @POST
    @Path("user")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = RecipeOpDescription.POST_PRIVATE, produces = ContentType.JSON, notes = Notes.RECIPE_NOTES,
            nickname = "postPrivateRecipe")
    RecipeResponse postPrivate(@Valid RecipeRequest recipeRequest);

    @POST
    @Path("account")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = RecipeOpDescription.POST_PUBLIC, produces = ContentType.JSON, notes = Notes.RECIPE_NOTES,
            nickname = "postPublicRecipe")
    RecipeResponse postPublic(@Valid RecipeRequest recipeRequest);

    @GET
    @Path("user")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = RecipeOpDescription.GET_PRIVATE, produces = ContentType.JSON, notes = Notes.RECIPE_NOTES,
            nickname = "getPrivatesRecipe")
    Set<RecipeResponse> getPrivates();

    @GET
    @Path("account")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = RecipeOpDescription.GET_PUBLIC, produces = ContentType.JSON, notes = Notes.RECIPE_NOTES,
            nickname = "getPublicsRecipe")
    Set<RecipeResponse> getPublics();

    @GET
    @Path("user/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = RecipeOpDescription.GET_PRIVATE_BY_NAME, produces = ContentType.JSON, notes = Notes.RECIPE_NOTES,
            nickname = "getPrivateRecipe")
    RecipeResponse getPrivate(@PathParam("name") String name);

    @GET
    @Path("account/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = RecipeOpDescription.GET_PUBLIC_BY_NAME, produces = ContentType.JSON, notes = Notes.RECIPE_NOTES,
            nickname = "getPublicRecipe")
    RecipeResponse getPublic(@PathParam("name") String name);

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = RecipeOpDescription.GET_BY_ID, produces = ContentType.JSON, notes = Notes.RECIPE_NOTES,
            nickname = "getRecipe")
    RecipeResponse get(@PathParam("id") Long id);

    @DELETE
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = RecipeOpDescription.DELETE_BY_ID, produces = ContentType.JSON, notes = Notes.RECIPE_NOTES,
            nickname = "deleteRecipe")
    void delete(@PathParam("id") Long id);

    @DELETE
    @Path("account/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = RecipeOpDescription.DELETE_PUBLIC_BY_NAME, produces = ContentType.JSON, notes = Notes.RECIPE_NOTES,
            nickname = "deletePublicRecipe")
    void deletePublic(@PathParam("name") String name);

    @DELETE
    @Path("user/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = RecipeOpDescription.DELETE_PRIVATE_BY_NAME, produces = ContentType.JSON, notes = Notes.RECIPE_NOTES,
            nickname = "deletePrivateRecipe")
    void deletePrivate(@PathParam("name") String name);
}
