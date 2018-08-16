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
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.RecipeOpDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v3/{organizationId}/recipes")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v3/{organizationId}/recipes", description = ControllerDescription.RECIPE_V3_DESCRIPTION, protocols = "http,https")
public interface RecipeV3Endpoint {

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = RecipeOpDescription.LIST_BY_ORGANIZATION, produces = ContentType.JSON, notes = Notes.RECIPE_NOTES,
            nickname = "listRecipesByOrganization")
    Set<RecipeResponse> listByOrganization(@PathParam("organizationId") Long organizationId);

    @GET
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = RecipeOpDescription.GET_BY_NAME_IN_ORG, produces = ContentType.JSON, notes = Notes.RECIPE_NOTES,
            nickname = "getRecipeInOrganization")
    RecipeResponse getByNameInOrganization(@PathParam("organizationId") Long organizationId, @PathParam("name") String name);

    @POST
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = RecipeOpDescription.CREATE_IN_ORG, produces = ContentType.JSON, notes = Notes.RECIPE_NOTES,
            nickname = "createRecipeInOrganization")
    RecipeResponse createInOrganization(@PathParam("organizationId") Long organizationId, @Valid RecipeRequest request);

    @DELETE
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = RecipeOpDescription.DELETE_BY_NAME_IN_ORG, produces = ContentType.JSON, notes = Notes.RECIPE_NOTES,
            nickname = "deleteRecipeInOrganization")
    RecipeResponse deleteInOrganization(@PathParam("organizationId") Long organizationId, @PathParam("name") String name);

    @GET
    @Path("{name}/request")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = RecipeOpDescription.GET_REQUEST_BY_NAME, produces = ContentType.JSON, notes = Notes.RECIPE_NOTES,
            nickname = "getRecipeRequestFromName")
    RecipeRequest getRequestFromName(@PathParam("organizationId") Long organizationId, @PathParam("name") String name);
}
