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

import com.sequenceiq.cloudbreak.api.model.mpack.ManagementPackRequest;
import com.sequenceiq.cloudbreak.api.model.mpack.ManagementPackResponse;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.ManagementPackOpDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v1/mpacks")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v1/mpacks", description = ControllerDescription.MANAGEMENT_PACK_DESCRIPTION, protocols = "http,https")
public interface ManagementPackEndpoint {
    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ManagementPackOpDescription.GET_BY_ID, produces = ContentType.JSON, notes = Notes.MANAGEMENT_PACK_NOTES,
            nickname = "getManagementPack")
    ManagementPackResponse get(@PathParam("id") Long id);

    @DELETE
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ManagementPackOpDescription.DELETE_BY_ID, produces = ContentType.JSON, notes = Notes.MANAGEMENT_PACK_NOTES,
            nickname = "deleteManagementPack")
    void delete(@PathParam("id") Long id);

    @POST
    @Path("user")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ManagementPackOpDescription.POST_PRIVATE, produces = ContentType.JSON, notes = Notes.MANAGEMENT_PACK_NOTES,
            nickname = "postPrivateManagementPack")
    ManagementPackResponse postPrivate(@Valid ManagementPackRequest mpackRequest);

    @GET
    @Path("user")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ManagementPackOpDescription.GET_PRIVATE, produces = ContentType.JSON, notes = Notes.MANAGEMENT_PACK_NOTES,
            nickname = "getPrivateManagementPacks")
    Set<ManagementPackResponse> getPrivates();

    @GET
    @Path("user/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ManagementPackOpDescription.GET_PRIVATE_BY_NAME, produces = ContentType.JSON, notes = Notes.MANAGEMENT_PACK_NOTES,
            nickname = "getPrivateManagementPack")
    ManagementPackResponse getPrivate(@PathParam("name") String name);

    @DELETE
    @Path("user/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ManagementPackOpDescription.DELETE_PRIVATE_BY_NAME, produces = ContentType.JSON, notes = Notes.MANAGEMENT_PACK_NOTES,
            nickname = "deletePrivateManagementPack")
    void deletePrivate(@PathParam("name") String name);

    @POST
    @Path("account")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ManagementPackOpDescription.POST_PUBLIC, produces = ContentType.JSON, notes = Notes.MANAGEMENT_PACK_NOTES,
            nickname = "postPublicManagementPack")
    ManagementPackResponse postPublic(@Valid ManagementPackRequest mpackRequest);

    @GET
    @Path("account")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ManagementPackOpDescription.GET_PUBLIC, produces = ContentType.JSON, notes = Notes.MANAGEMENT_PACK_NOTES,
            nickname = "getPublicManagementPacks")
    Set<ManagementPackResponse> getPublics();

    @GET
    @Path("account/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ManagementPackOpDescription.GET_PUBLIC_BY_NAME, produces = ContentType.JSON, notes = Notes.MANAGEMENT_PACK_NOTES,
            nickname = "getPublicManagementPack")
    ManagementPackResponse getPublic(@PathParam("name") String name);

    @DELETE
    @Path("account/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ManagementPackOpDescription.DELETE_PUBLIC_BY_NAME, produces = ContentType.JSON, notes = Notes.MANAGEMENT_PACK_NOTES,
            nickname = "deletePublicManagementPack")
    void deletePublic(@PathParam("name") String name);
}
