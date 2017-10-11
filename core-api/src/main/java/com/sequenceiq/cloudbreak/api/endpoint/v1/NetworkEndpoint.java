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

import com.sequenceiq.cloudbreak.api.model.NetworkRequest;
import com.sequenceiq.cloudbreak.api.model.NetworkResponse;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.NetworkOpDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v1/networks")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v1/networks", description = ControllerDescription.NETWORK_DESCRIPTION, protocols = "http,https")
public interface NetworkEndpoint {

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = NetworkOpDescription.GET_BY_ID, produces = ContentType.JSON, notes = Notes.NETWORK_NOTES,
            nickname = "getNetwork")
    NetworkResponse get(@PathParam("id") Long id);

    @DELETE
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = NetworkOpDescription.DELETE_BY_ID, produces = ContentType.JSON, notes = Notes.NETWORK_NOTES,
            nickname = "deleteNetwork")
    void delete(@PathParam("id") Long id);

    @POST
    @Path("account")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = NetworkOpDescription.POST_PUBLIC, produces = ContentType.JSON, notes = Notes.NETWORK_NOTES,
            nickname = "postPublicNetwork")
    NetworkResponse postPublic(@Valid NetworkRequest networkRequest);

    @GET
    @Path("account")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = NetworkOpDescription.GET_PUBLIC, produces = ContentType.JSON, notes = Notes.NETWORK_NOTES,
            nickname = "getPublicsNetwork")
    Set<NetworkResponse> getPublics();

    @GET
    @Path("account/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = NetworkOpDescription.GET_PUBLIC_BY_NAME, produces = ContentType.JSON, notes = Notes.NETWORK_NOTES,
            nickname = "getPublicNetwork")
    NetworkResponse getPublic(@PathParam("name") String name);

    @DELETE
    @Path("account/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = NetworkOpDescription.DELETE_PUBLIC_BY_NAME, produces = ContentType.JSON, notes = Notes.NETWORK_NOTES,
            nickname = "deletePublicNetwork")
    void deletePublic(@PathParam("name") String name);

    @POST
    @Path("user")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = NetworkOpDescription.POST_PRIVATE, produces = ContentType.JSON, notes = Notes.NETWORK_NOTES,
            nickname = "postPrivateNetwork")
    NetworkResponse postPrivate(@Valid NetworkRequest networkRequest);

    @GET
    @Path("user")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = NetworkOpDescription.GET_PRIVATE, produces = ContentType.JSON, notes = Notes.NETWORK_NOTES,
            nickname = "getPrivatesNetwork")
    Set<NetworkResponse> getPrivates();

    @GET
    @Path("user/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = NetworkOpDescription.GET_PRIVATE_BY_NAME, produces = ContentType.JSON, notes = Notes.NETWORK_NOTES,
            nickname = "getPrivateNetwork")
    NetworkResponse getPrivate(@PathParam("name") String name);

    @DELETE
    @Path("user/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = NetworkOpDescription.DELETE_PRIVATE_BY_NAME, produces = ContentType.JSON, notes = Notes.NETWORK_NOTES,
            nickname = "deletePrivateNetwork")
    void deletePrivate(@PathParam("name") String name);

}
