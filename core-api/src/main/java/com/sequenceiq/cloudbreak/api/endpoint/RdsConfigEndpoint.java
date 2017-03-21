package com.sequenceiq.cloudbreak.api.endpoint;

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

import com.sequenceiq.cloudbreak.api.model.RDSConfigJson;
import com.sequenceiq.cloudbreak.api.model.RDSConfigResponse;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/rdsconfigs")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/rdsconfigs", description = ControllerDescription.RDSCONFIG_DESCRIPTION, protocols = "http,https")
public interface RdsConfigEndpoint {

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.RdsConfigOpDescription.GET_BY_ID, produces = ContentType.JSON, notes = Notes.RDSCONFIG_NOTES,
            nickname = "getRds")
    RDSConfigResponse get(@PathParam(value = "id") Long id);

    @DELETE
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.RdsConfigOpDescription.DELETE_BY_ID, produces = ContentType.JSON, notes = Notes.RDSCONFIG_NOTES,
            nickname = "deleteRds")
    void delete(@PathParam(value = "id") Long id);

    @POST
    @Path("user")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.RdsConfigOpDescription.POST_PRIVATE, produces = ContentType.JSON, notes = Notes.RDSCONFIG_NOTES,
            nickname = "postPrivateRds")
    RDSConfigResponse postPrivate(@Valid RDSConfigJson rdsConfigJson);

    @GET
    @Path("user")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.RdsConfigOpDescription.GET_PRIVATE, produces = ContentType.JSON, notes = Notes.RDSCONFIG_NOTES,
            nickname = "getPrivatesRds")
    Set<RDSConfigResponse> getPrivates();

    @GET
    @Path("user/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.RdsConfigOpDescription.GET_PRIVATE_BY_NAME, produces = ContentType.JSON, notes = Notes.RDSCONFIG_NOTES,
            nickname = "getPrivateRds")
    RDSConfigJson getPrivate(@PathParam(value = "name") String name);

    @DELETE
    @Path("user/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.RdsConfigOpDescription.DELETE_PRIVATE_BY_NAME, produces = ContentType.JSON, notes = Notes.RDSCONFIG_NOTES,
            nickname = "deletePrivateRds")
    void deletePrivate(@PathParam(value = "name") String name);

    @POST
    @Path("account")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.RdsConfigOpDescription.POST_PUBLIC, produces = ContentType.JSON, notes = Notes.RDSCONFIG_NOTES,
            nickname = "potPublicRds")
    RDSConfigResponse postPublic(@Valid RDSConfigJson rdsConfigJson);

    @GET
    @Path("account")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.RdsConfigOpDescription.GET_PUBLIC, produces = ContentType.JSON, notes = Notes.RDSCONFIG_NOTES,
            nickname = "getPublicsRds")
    Set<RDSConfigResponse> getPublics();

    @GET
    @Path("account/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.RdsConfigOpDescription.GET_PUBLIC_BY_NAME, produces = ContentType.JSON, notes = Notes.RDSCONFIG_NOTES,
            nickname = "getPublicRds")
    RDSConfigResponse getPublic(@PathParam(value = "name") String name);

    @DELETE
    @Path("account/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.RdsConfigOpDescription.DELETE_PUBLIC_BY_NAME, produces = ContentType.JSON, notes = Notes.RDSCONFIG_NOTES,
            nickname = "deletePublicRds")
    void deletePublic(@PathParam(value = "name") String name);

}
