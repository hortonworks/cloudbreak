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

import com.sequenceiq.cloudbreak.api.model.NetworkRequest;
import com.sequenceiq.cloudbreak.api.model.NetworkResponse;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/networks")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/networks", description = ControllerDescription.NETWORK_DESCRIPTION, protocols = "http, https")
public interface NetworkEndpoint {

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.NetworkOpDescription.GET_BY_ID, produces = ContentType.JSON, notes = Notes.NETWORK_NOTES)
    NetworkResponse get(@PathParam("id") Long id);

    @DELETE
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.NetworkOpDescription.DELETE_BY_ID, produces = ContentType.JSON, notes = Notes.NETWORK_NOTES)
    void delete(@PathParam(value = "id") Long id);

    @POST
    @Path("account")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.NetworkOpDescription.POST_PUBLIC, produces = ContentType.JSON, notes = Notes.NETWORK_NOTES)
    NetworkResponse postPublic(@Valid NetworkRequest networkRequest);

    @GET
    @Path("account")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.NetworkOpDescription.GET_PUBLIC, produces = ContentType.JSON, notes = Notes.NETWORK_NOTES)
    Set<NetworkResponse> getPublics();

    @GET
    @Path("account/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.NetworkOpDescription.GET_PUBLIC_BY_NAME, produces = ContentType.JSON, notes = Notes.NETWORK_NOTES)
    NetworkResponse getPublic(@PathParam(value = "name") String name);

    @DELETE
    @Path("account/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.NetworkOpDescription.DELETE_PUBLIC_BY_NAME, produces = ContentType.JSON, notes = Notes.NETWORK_NOTES)
    void deletePublic(@PathParam(value = "name") String name);

    @POST
    @Path("user")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.NetworkOpDescription.POST_PRIVATE, produces = ContentType.JSON, notes = Notes.NETWORK_NOTES)
    NetworkResponse postPrivate(@Valid NetworkRequest networkRequest);

    @GET
    @Path("user")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.NetworkOpDescription.GET_PRIVATE, produces = ContentType.JSON, notes = Notes.NETWORK_NOTES)
    Set<NetworkResponse> getPrivates();

    @GET
    @Path("user/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.NetworkOpDescription.GET_PRIVATE_BY_NAME, produces = ContentType.JSON, notes = Notes.NETWORK_NOTES)
    NetworkResponse getPrivate(@PathParam(value = "name") String name);

    @DELETE
    @Path("user/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.NetworkOpDescription.DELETE_PRIVATE_BY_NAME, produces = ContentType.JSON, notes = Notes.NETWORK_NOTES)
    void deletePrivate(@PathParam(value = "name") String name);

}
