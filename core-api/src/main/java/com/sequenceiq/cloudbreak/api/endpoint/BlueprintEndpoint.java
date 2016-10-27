package com.sequenceiq.cloudbreak.api.endpoint;

import static com.sequenceiq.cloudbreak.doc.ContentType.JSON;
import static com.sequenceiq.cloudbreak.doc.Notes.BLUEPRINT_NOTES;

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

import com.sequenceiq.cloudbreak.api.model.BlueprintRequest;
import com.sequenceiq.cloudbreak.api.model.BlueprintResponse;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/blueprints")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/blueprints", description = ControllerDescription.BLUEPRINT_DESCRIPTION, protocols = "http,https")
public interface BlueprintEndpoint {

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.BlueprintOpDescription.GET_BY_ID, produces = JSON, notes = BLUEPRINT_NOTES)
    BlueprintResponse get(@PathParam(value = "id") Long id);

    @DELETE
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.BlueprintOpDescription.DELETE_BY_ID, produces = JSON, notes = BLUEPRINT_NOTES)
    void delete(@PathParam(value = "id") Long id);

    @POST
    @Path("user")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.BlueprintOpDescription.POST_PRIVATE, produces = JSON, notes = BLUEPRINT_NOTES)
    BlueprintResponse postPrivate(@Valid BlueprintRequest blueprintRequest);

    @GET
    @Path("user")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.BlueprintOpDescription.GET_PRIVATE, produces = JSON, notes = BLUEPRINT_NOTES)
    Set<BlueprintResponse> getPrivates();

    @GET
    @Path("user/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.BlueprintOpDescription.GET_PRIVATE_BY_NAME, produces = JSON, notes = BLUEPRINT_NOTES)
    BlueprintResponse getPrivate(@PathParam(value = "name") String name);

    @DELETE
    @Path("user/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.BlueprintOpDescription.DELETE_PRIVATE_BY_NAME, produces = JSON, notes = BLUEPRINT_NOTES)
    void deletePrivate(@PathParam(value = "name") String name);

    @POST
    @Path("account")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.BlueprintOpDescription.POST_PUBLIC, produces = JSON, notes = BLUEPRINT_NOTES)
    BlueprintResponse postPublic(@Valid BlueprintRequest blueprintRequest);

    @GET
    @Path("account")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.BlueprintOpDescription.GET_PUBLIC, produces = JSON, notes = BLUEPRINT_NOTES)
    Set<BlueprintResponse> getPublics();

    @GET
    @Path("account/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.BlueprintOpDescription.GET_PUBLIC_BY_NAME, produces = JSON, notes = BLUEPRINT_NOTES)
    BlueprintResponse getPublic(@PathParam(value = "name") String name);

    @DELETE
    @Path("account/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.BlueprintOpDescription.DELETE_PUBLIC_BY_NAME, produces = JSON, notes = BLUEPRINT_NOTES)
    void deletePublic(@PathParam(value = "name") String name);

}
