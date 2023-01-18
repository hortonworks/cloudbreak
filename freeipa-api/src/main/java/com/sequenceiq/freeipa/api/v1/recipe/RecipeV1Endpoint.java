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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RetryAndMetrics
@Path("/v1/recipe")
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "/v1/recipe", description = RecipeDescription.NOTES)
public interface RecipeV1Endpoint {

    @POST
    @Path("attach")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = RecipeDescription.ATTACH, operationId = "attachRecipe",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    void attachRecipes(@Valid RecipeAttachDetachRequest recipeAttach);

    @POST
    @Path("detach")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = RecipeDescription.DETACH, operationId = "detachRecipe",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    void detachRecipes(@Valid RecipeAttachDetachRequest recipeAttach);
}
