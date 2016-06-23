package com.sequenceiq.cloudbreak.api.endpoint

import javax.validation.Valid
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

import com.sequenceiq.cloudbreak.doc.ContentType
import com.sequenceiq.cloudbreak.doc.ControllerDescription
import com.sequenceiq.cloudbreak.doc.Notes
import com.sequenceiq.cloudbreak.doc.OperationDescriptions
import com.sequenceiq.cloudbreak.api.model.IdJson
import com.sequenceiq.cloudbreak.api.model.RecipeRequest
import com.sequenceiq.cloudbreak.api.model.RecipeResponse

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation

@Path("/recipes")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/recipes", description = ControllerDescription.RECIPE_DESCRIPTION, position = 5)
interface RecipeEndpoint {

    @POST
    @Path("user")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.RecipeOpDescription.POST_PRIVATE, produces = ContentType.JSON, notes = Notes.RECIPE_NOTES)
    fun postPrivate(@Valid recipeRequest: RecipeRequest): IdJson

    @POST
    @Path("account")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.RecipeOpDescription.POST_PUBLIC, produces = ContentType.JSON, notes = Notes.RECIPE_NOTES)
    fun postPublic(@Valid recipeRequest: RecipeRequest): IdJson

    val privates: Set<RecipeResponse>

    val publics: Set<RecipeResponse>

    @GET
    @Path("user/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.RecipeOpDescription.GET_PRIVATE_BY_NAME, produces = ContentType.JSON, notes = Notes.RECIPE_NOTES)
    fun getPrivate(@PathParam(value = "name") name: String): RecipeResponse

    @GET
    @Path("account/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.RecipeOpDescription.GET_PUBLIC_BY_NAME, produces = ContentType.JSON, notes = Notes.RECIPE_NOTES)
    fun getPublic(@PathParam(value = "name") name: String): RecipeResponse

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.RecipeOpDescription.GET_BY_ID, produces = ContentType.JSON, notes = Notes.RECIPE_NOTES)
    operator fun get(@PathParam(value = "id") id: Long?): RecipeResponse

    @DELETE
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.RecipeOpDescription.DELETE_BY_ID, produces = ContentType.JSON, notes = Notes.RECIPE_NOTES)
    fun delete(@PathParam(value = "id") id: Long?)

    @DELETE
    @Path("account/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.RecipeOpDescription.DELETE_PUBLIC_BY_NAME, produces = ContentType.JSON, notes = Notes.RECIPE_NOTES)
    fun deletePublic(@PathParam(value = "name") name: String)

    @DELETE
    @Path("user/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.RecipeOpDescription.DELETE_PRIVATE_BY_NAME, produces = ContentType.JSON, notes = Notes.RECIPE_NOTES)
    fun deletePrivate(@PathParam(value = "name") name: String)
}
