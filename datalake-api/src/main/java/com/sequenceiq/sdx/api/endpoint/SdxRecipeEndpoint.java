package com.sequenceiq.sdx.api.endpoint;

import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.ATTACH_RECIPE_BY_CRN;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.ATTACH_RECIPE_BY_NAME;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.DETACH_RECIPE_BY_CRN;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.DETACH_RECIPE_BY_NAME;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.REFRESH_RECIPES_BY_CRN;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.REFRESH_RECIPES_BY_NAME;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.springframework.validation.annotation.Validated;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.recipe.AttachRecipeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.recipe.DetachRecipeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.recipe.UpdateRecipesV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.recipe.AttachRecipeV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.recipe.DetachRecipeV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.recipe.UpdateRecipesV4Response;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Validated
@Path("/sdx")
@RetryAndMetrics
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/sdx", protocols = "http,https", consumes = MediaType.APPLICATION_JSON)
public interface SdxRecipeEndpoint {

    @PUT
    @Path("crn/{crn}/refresh_recipes")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = REFRESH_RECIPES_BY_CRN, nickname = "refreshRecipesByCrn")
    UpdateRecipesV4Response refreshRecipesByCrn(@PathParam("crn") String crn, @Valid UpdateRecipesV4Request request);

    @PUT
    @Path("name/{name}/refresh_recipes")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = REFRESH_RECIPES_BY_NAME, nickname = "refreshRecipesByName")
    UpdateRecipesV4Response refreshRecipesByName(@PathParam("name") String name, @Valid UpdateRecipesV4Request request);

    @POST
    @Path("crn/{crn}/attach_recipe")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ATTACH_RECIPE_BY_CRN, nickname = "attachRecipesByCrn")
    AttachRecipeV4Response attachRecipeByCrn(@PathParam("crn") String crn, @Valid AttachRecipeV4Request request);

    @POST
    @Path("{name}/attach_recipe")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ATTACH_RECIPE_BY_NAME, nickname = "attachRecipesByName")
    AttachRecipeV4Response attachRecipeByName(@PathParam("name") String name, @Valid AttachRecipeV4Request request);

    @POST
    @Path("crn/{crn}/detach_recipe")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DETACH_RECIPE_BY_CRN, nickname = "detachRecipesByCrn")
    DetachRecipeV4Response detachRecipeByCrn(@PathParam("crn") String crn, @Valid DetachRecipeV4Request request);

    @POST
    @Path("{name}/detach_recipe")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DETACH_RECIPE_BY_NAME, nickname = "detachRecipesByName")
    DetachRecipeV4Response detachRecipeByName(@PathParam("name") String name, @Valid DetachRecipeV4Request request);
}
