package com.sequenceiq.cloudbreak.api.endpoint.v4.environment;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.requests.DatalakePrerequisiteV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.requests.EnvironmentAttachV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.requests.EnvironmentChangeCredentialV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.requests.EnvironmentDetachV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.requests.EnvironmentEditV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.requests.EnvironmentV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.requests.RegisterDatalakeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.responses.DatalakePrerequisiteV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.responses.DetailedEnvironmentV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.responses.SimpleEnvironmentV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.responses.SimpleEnvironmentV4Responses;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.EnvironmentOpDescription;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.UtilityOpDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v4/{workspaceId}/environments")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v4/{workspaceId}/environments", description = ControllerDescription.ENVIRONMENT_V4_DESCRIPTION, protocols = "http,https")
public interface EnvironmentV4Endpoint {

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = EnvironmentOpDescription.CREATE, produces = ContentType.JSON, notes = Notes.ENVIRONMENT_NOTES,
            nickname = "createEnvironment")
    DetailedEnvironmentV4Response post(@PathParam("workspaceId") Long workspaceId, @Valid EnvironmentV4Request request);

    @GET
    @Path("/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = EnvironmentOpDescription.GET, produces = ContentType.JSON, notes = Notes.ENVIRONMENT_NOTES,
            nickname = "getEnvironment")
    DetailedEnvironmentV4Response get(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String environmentName);

    @DELETE
    @Path("/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = EnvironmentOpDescription.DELETE, produces = ContentType.JSON, notes = Notes.ENVIRONMENT_NOTES,
            nickname = "deleteEnvironment")
    SimpleEnvironmentV4Response delete(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String environmentName);

    @PUT
    @Path("/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = EnvironmentOpDescription.EDIT, produces = ContentType.JSON, notes = Notes.ENVIRONMENT_NOTES,
            nickname = "editEnvironment")
    DetailedEnvironmentV4Response edit(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String environmentName,
            @NotNull EnvironmentEditV4Request request);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = EnvironmentOpDescription.LIST, produces = ContentType.JSON, notes = Notes.ENVIRONMENT_NOTES,
            nickname = "listEnvironment")
    SimpleEnvironmentV4Responses list(@PathParam("workspaceId") Long workspaceId);

    @PUT
    @Path("/{name}/attach")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = EnvironmentOpDescription.ATTACH_RESOURCES, produces = ContentType.JSON, notes = Notes.ENVIRONMENT_NOTES,
            nickname = "attachResourcesToEnvironment")
    DetailedEnvironmentV4Response attach(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String environmentName,
            @Valid EnvironmentAttachV4Request request);

    @PUT
    @Path("/{name}/detach")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = EnvironmentOpDescription.DETACH_RESOURCES, produces = ContentType.JSON, notes = Notes.ENVIRONMENT_NOTES,
            nickname = "detachResourcesFromEnvironment")
    DetailedEnvironmentV4Response detach(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String environmentName,
            @Valid EnvironmentDetachV4Request request);

    @PUT
    @Path("/{name}/change_credential")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = EnvironmentOpDescription.CHANGE_CREDENTIAL, produces = ContentType.JSON, notes = Notes.ENVIRONMENT_NOTES,
            nickname = "changeCredentialInEnvironment")
    DetailedEnvironmentV4Response changeCredential(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String environmentName,
        @Valid EnvironmentChangeCredentialV4Request request);

    @PUT
    @Path("/{name}/register_datalake")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = EnvironmentOpDescription.REGISTER_EXTERNAL_DATALAKE, produces = ContentType.JSON, notes = Notes.ENVIRONMENT_NOTES,
            nickname = "registerExternalDatalake")
    DetailedEnvironmentV4Response registerExternalDatalake(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String environmentName,
        @Valid RegisterDatalakeV4Request request);

    @POST
    @Path("/{name}/register_datalake_prerequisites")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = UtilityOpDescription.DATALAKE_PREREQUISITES, produces = ContentType.JSON,
            nickname = "registerDatalakePrerequisites")
    DatalakePrerequisiteV4Response registerDatalakePrerequisite(
            @PathParam("workspaceId") Long workspaceId,
            @PathParam("name") String environmentName,
            @Valid DatalakePrerequisiteV4Request datalakePrerequisiteV4Request);
}
