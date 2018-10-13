package com.sequenceiq.cloudbreak.api.endpoint.v3;

import java.util.Set;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.api.model.environment.request.EnvironmentAttachRequest;
import com.sequenceiq.cloudbreak.api.model.environment.request.EnvironmentDetachRequest;
import com.sequenceiq.cloudbreak.api.model.environment.request.EnvironmentRequest;
import com.sequenceiq.cloudbreak.api.model.environment.response.DetailedEnvironmentResponse;
import com.sequenceiq.cloudbreak.api.model.environment.response.SimpleEnvironmentResponse;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.EnvironmentOpDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v3/{workspaceId}/environments")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v3/{workspaceId}/environments", description = ControllerDescription.ENVIRONMENT_V3_DESCRIPTION, protocols = "http,https")
public interface EnvironmentV3Endpoint {

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = EnvironmentOpDescription.CREATE, produces = ContentType.JSON, notes = Notes.ENVIRONMENT_NOTES,
            nickname = "create")
    DetailedEnvironmentResponse create(@PathParam("workspaceId") Long workspaceId, @Valid EnvironmentRequest request);

    @GET
    @Path("/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @QueryParam("")
    @ApiOperation(value = EnvironmentOpDescription.GET, produces = ContentType.JSON, notes = Notes.ENVIRONMENT_NOTES,
            nickname = "get")
    DetailedEnvironmentResponse get(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String environmentName);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = EnvironmentOpDescription.LIST, produces = ContentType.JSON, notes = Notes.ENVIRONMENT_NOTES,
            nickname = "list")
    Set<SimpleEnvironmentResponse> list(@PathParam("workspaceId") Long workspaceId);

    @PUT
    @Path("/{name}/attach")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = EnvironmentOpDescription.ATTACH_RESOURCES, produces = ContentType.JSON, notes = Notes.ENVIRONMENT_NOTES,
            nickname = "attachResources")
    DetailedEnvironmentResponse attachResources(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String environmentName,
            @Valid EnvironmentAttachRequest request);

    @PUT
    @Path("/{name}/detach")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = EnvironmentOpDescription.DETACH_RESOURCES, produces = ContentType.JSON, notes = Notes.ENVIRONMENT_NOTES,
            nickname = "detachResources")
    DetailedEnvironmentResponse detachResources(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String environmentName,
            @Valid EnvironmentDetachRequest request);
}