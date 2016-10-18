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

import com.sequenceiq.cloudbreak.api.model.SssdConfigRequest;
import com.sequenceiq.cloudbreak.api.model.SssdConfigResponse;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/sssd")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/sssd", description = ControllerDescription.SSSDCONFIG_DESCRIPTION, protocols = "http, https")
public interface SssdConfigEndpoint {

    @POST
    @Path("user")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.SssdConfigOpDescription.POST_PRIVATE, produces = ContentType.JSON, notes = Notes.SSSDCONFIG_NOTES)
    SssdConfigResponse postPrivate(@Valid SssdConfigRequest sssdConfigRequest);

    @POST
    @Path("account")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.SssdConfigOpDescription.POST_PUBLIC, produces = ContentType.JSON, notes = Notes.SSSDCONFIG_NOTES)
    SssdConfigResponse postPublic(@Valid SssdConfigRequest sssdConfigRequest);

    @GET
    @Path("user")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.SssdConfigOpDescription.GET_PRIVATE, produces = ContentType.JSON, notes = Notes.SSSDCONFIG_NOTES)
    Set<SssdConfigResponse> getPrivates();

    @GET
    @Path("account")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.SssdConfigOpDescription.GET_PUBLIC, produces = ContentType.JSON, notes = Notes.SSSDCONFIG_NOTES)
    Set<SssdConfigResponse> getPublics();

    @GET
    @Path("user/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.SssdConfigOpDescription.GET_PRIVATE_BY_NAME, produces = ContentType.JSON, notes = Notes.SSSDCONFIG_NOTES)
    SssdConfigResponse getPrivate(@PathParam(value = "name") String name);

    @GET
    @Path("account/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.SssdConfigOpDescription.GET_PUBLIC_BY_NAME, produces = ContentType.JSON, notes = Notes.SSSDCONFIG_NOTES)
    SssdConfigResponse getPublic(@PathParam(value = "name") String name);

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.SssdConfigOpDescription.GET_BY_ID, produces = ContentType.JSON, notes = Notes.SSSDCONFIG_NOTES)
    SssdConfigResponse get(@PathParam(value = "id") Long id);

    @DELETE
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.SssdConfigOpDescription.DELETE_BY_ID, produces = ContentType.JSON, notes = Notes.SSSDCONFIG_NOTES)
    void delete(@PathParam(value = "id") Long id);

    @DELETE
    @Path("account/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.SssdConfigOpDescription.DELETE_PUBLIC_BY_NAME, produces = ContentType.JSON, notes = Notes.SSSDCONFIG_NOTES)
    void deletePublic(@PathParam(value = "name") String name);

    @DELETE
    @Path("user/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.SssdConfigOpDescription.DELETE_PRIVATE_BY_NAME, produces = ContentType.JSON, notes = Notes.SSSDCONFIG_NOTES)
    void deletePrivate(@PathParam(value = "name") String name);
}
