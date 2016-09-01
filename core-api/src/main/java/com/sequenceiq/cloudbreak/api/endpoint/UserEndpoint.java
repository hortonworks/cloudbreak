package com.sequenceiq.cloudbreak.api.endpoint;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.api.model.UserRequest;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/users")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/users", description = ControllerDescription.USER_DESCRIPTION)
public interface UserEndpoint {

    @PUT
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.UserOpDescription.USER_DETAILS_EVICT, produces = ContentType.JSON, notes = Notes.USER_NOTES)
    String evictUserDetails(@PathParam(value = "id") String id, @Valid UserRequest userRequest);

    @GET
    @Path("{id}/resources")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.UserOpDescription.USER_GET_RESOURCE, produces = ContentType.JSON, notes = Notes.USER_NOTES)
    Boolean hasResources(@PathParam(value = "id") String id);
}
