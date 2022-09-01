package com.sequenceiq.freeipa.api.v1.recipe;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.freeipa.api.v1.recipe.doc.RecipeDescription;
import com.sequenceiq.freeipa.api.v1.recipe.model.RecipeAttachDetachRequest;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@RetryAndMetrics
@Path("/v1/recipe")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v1/recipe", description = RecipeDescription.NOTES, protocols = "http,https", consumes = MediaType.APPLICATION_JSON)
public interface RecipeV1Endpoint {

    @POST
    @Path("attach")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = RecipeDescription.ATTACH, produces = MediaType.APPLICATION_JSON, nickname = "attachRecipe")
    void attachRecipes(@Valid RecipeAttachDetachRequest recipeAttach);

    @POST
    @Path("detach")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = RecipeDescription.DETACH, produces = MediaType.APPLICATION_JSON, nickname = "detachRecipe")
    void detachRecipes(@Valid RecipeAttachDetachRequest recipeAttach);
}
