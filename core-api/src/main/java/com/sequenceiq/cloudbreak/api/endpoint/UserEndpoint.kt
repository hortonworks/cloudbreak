package com.sequenceiq.cloudbreak.api.endpoint

import javax.validation.Valid
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

import com.sequenceiq.cloudbreak.doc.ContentType
import com.sequenceiq.cloudbreak.doc.ControllerDescription
import com.sequenceiq.cloudbreak.doc.Notes
import com.sequenceiq.cloudbreak.doc.OperationDescriptions
import com.sequenceiq.cloudbreak.api.model.UserRequest
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation

@Path("/users")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/users", description = ControllerDescription.USER_DESCRIPTION, position = 9)
interface UserEndpoint {

    @PUT
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.UserOpDescription.USER_DETAILS_EVICT, produces = ContentType.JSON, notes = Notes.USER_NOTES)
    fun evictUserDetails(@PathParam(value = "id") id: String, @Valid userRequest: UserRequest): String

    @GET
    @Path("{id}/resources")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.UserOpDescription.USER_GET_RESOURCE, produces = ContentType.JSON, notes = Notes.USER_NOTES)
    fun hasResources(@PathParam(value = "id") id: String): Boolean?
}
