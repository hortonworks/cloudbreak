package com.sequenceiq.cloudbreak.api.endpoint.v1;

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
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.BlueprintOpDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v1/blueprints")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v1/blueprints", description = ControllerDescription.BLUEPRINT_DESCRIPTION, protocols = "http,https")
public interface BlueprintEndpoint {

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = BlueprintOpDescription.GET_BY_ID, produces = JSON, notes = BLUEPRINT_NOTES, nickname = "getBlueprint")
    BlueprintResponse get(@PathParam("id") Long id);

    @DELETE
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = BlueprintOpDescription.DELETE_BY_ID, produces = JSON, notes = BLUEPRINT_NOTES, nickname = "deleteBlueprint")
    void delete(@PathParam("id") Long id);

    @POST
    @Path("user")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = BlueprintOpDescription.POST_PRIVATE, produces = JSON, notes = BLUEPRINT_NOTES,
            nickname = "postPrivateBlueprint")
    BlueprintResponse postPrivate(@Valid BlueprintRequest blueprintRequest);

    @GET
    @Path("user")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = BlueprintOpDescription.GET_PRIVATE, produces = JSON, notes = BLUEPRINT_NOTES,
            nickname = "getPrivatesBlueprint")
    Set<BlueprintResponse> getPrivates();

    @GET
    @Path("user/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = BlueprintOpDescription.GET_PRIVATE_BY_NAME, produces = JSON, notes = BLUEPRINT_NOTES,
            nickname = "getPrivateBlueprint")
    BlueprintResponse getPrivate(@PathParam("name") String name);

    @DELETE
    @Path("user/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = BlueprintOpDescription.DELETE_PRIVATE_BY_NAME, produces = JSON, notes = BLUEPRINT_NOTES,
            nickname = "deletePrivateBlueprint")
    void deletePrivate(@PathParam("name") String name);

    @POST
    @Path("account")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = BlueprintOpDescription.POST_PUBLIC, produces = JSON, notes = BLUEPRINT_NOTES,
            nickname = "postPublicBlueprint")
    BlueprintResponse postPublic(@Valid BlueprintRequest blueprintRequest);

    @GET
    @Path("account")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = BlueprintOpDescription.GET_PUBLIC, produces = JSON, notes = BLUEPRINT_NOTES,
            nickname = "getPublicsBlueprint")
    Set<BlueprintResponse> getPublics();

    @GET
    @Path("account/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = BlueprintOpDescription.GET_PUBLIC_BY_NAME, produces = JSON, notes = BLUEPRINT_NOTES,
            nickname = "getPublicBlueprint")
    BlueprintResponse getPublic(@PathParam("name") String name);

    @DELETE
    @Path("account/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = BlueprintOpDescription.DELETE_PUBLIC_BY_NAME, produces = JSON, notes = BLUEPRINT_NOTES,
            nickname = "deletePublicBlueprint")
    void deletePublic(@PathParam("name") String name);

}
