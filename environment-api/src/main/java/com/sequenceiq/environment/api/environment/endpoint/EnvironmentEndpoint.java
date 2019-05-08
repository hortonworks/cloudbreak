package com.sequenceiq.environment.api.environment.endpoint;

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

import com.sequenceiq.environment.api.WelcomeResponse;
import com.sequenceiq.environment.api.environment.doc.EnvironmentDescription;
import com.sequenceiq.environment.api.environment.model.requests.DatalakePrerequisiteV4Request;
import com.sequenceiq.environment.api.environment.model.requests.EnvironmentAttachV4Request;
import com.sequenceiq.environment.api.environment.model.requests.EnvironmentChangeCredentialV4Request;
import com.sequenceiq.environment.api.environment.model.requests.EnvironmentDetachV4Request;
import com.sequenceiq.environment.api.environment.model.requests.EnvironmentEditV4Request;
import com.sequenceiq.environment.api.environment.model.requests.EnvironmentV4Request;
import com.sequenceiq.environment.api.environment.model.requests.RegisterDatalakeV4Request;
import com.sequenceiq.environment.api.environment.model.responses.DatalakePrerequisiteV4Response;
import com.sequenceiq.environment.api.environment.model.responses.DetailedEnvironmentV4Response;
import com.sequenceiq.environment.api.environment.model.responses.SimpleEnvironmentV4Response;
import com.sequenceiq.environment.api.environment.model.responses.SimpleEnvironmentV4Responses;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/env")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/env", protocols = "http,https")
public interface EnvironmentEndpoint {

    @GET
    @Path("welcome")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "welcome", produces = MediaType.APPLICATION_JSON, nickname = "getWelcomeMessage")
    WelcomeResponse welcome();

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = EnvironmentDescription.CREATE, produces = MediaType.APPLICATION_JSON, notes = EnvironmentDescription.ENVIRONMENT_NOTES,
            nickname = "createEnvironment")
    DetailedEnvironmentV4Response post(@PathParam("workspaceId") Long workspaceId, @Valid EnvironmentV4Request request);

    @GET
    @Path("/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = EnvironmentDescription.GET, produces = MediaType.APPLICATION_JSON, notes = EnvironmentDescription.ENVIRONMENT_NOTES,
            nickname = "getEnvironment")
    DetailedEnvironmentV4Response get(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String environmentName);

    @DELETE
    @Path("/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = EnvironmentDescription.DELETE, produces = MediaType.APPLICATION_JSON, notes = EnvironmentDescription.ENVIRONMENT_NOTES,
            nickname = "deleteEnvironment")
    SimpleEnvironmentV4Response delete(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String environmentName);

    @PUT
    @Path("/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = EnvironmentDescription.EDIT, produces = MediaType.APPLICATION_JSON, notes = EnvironmentDescription.ENVIRONMENT_NOTES,
            nickname = "editEnvironment")
    DetailedEnvironmentV4Response edit(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String environmentName,
            @NotNull EnvironmentEditV4Request request);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = EnvironmentDescription.LIST, produces = MediaType.APPLICATION_JSON, notes = EnvironmentDescription.ENVIRONMENT_NOTES,
            nickname = "listEnvironment")
    SimpleEnvironmentV4Responses list(@PathParam("workspaceId") Long workspaceId);

    @PUT
    @Path("/{name}/attach")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = EnvironmentDescription.ATTACH_RESOURCES, produces = MediaType.APPLICATION_JSON, notes = EnvironmentDescription.ENVIRONMENT_NOTES,
            nickname = "attachResourcesToEnvironment")
    DetailedEnvironmentV4Response attach(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String environmentName,
            @Valid EnvironmentAttachV4Request request);

    @PUT
    @Path("/{name}/detach")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = EnvironmentDescription.DETACH_RESOURCES, produces = MediaType.APPLICATION_JSON, notes = EnvironmentDescription.ENVIRONMENT_NOTES,
            nickname = "detachResourcesFromEnvironment")
    DetailedEnvironmentV4Response detach(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String environmentName,
            @Valid EnvironmentDetachV4Request request);

    @PUT
    @Path("/{name}/change_credential")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = EnvironmentDescription.CHANGE_CREDENTIAL, produces = MediaType.APPLICATION_JSON, notes = EnvironmentDescription.ENVIRONMENT_NOTES,
            nickname = "changeCredentialInEnvironment")
    DetailedEnvironmentV4Response changeCredential(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String environmentName,
            @Valid EnvironmentChangeCredentialV4Request request);

    @PUT
    @Path("/{name}/register_datalake")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = EnvironmentDescription.REGISTER_EXTERNAL_DATALAKE, produces = MediaType.APPLICATION_JSON, notes = EnvironmentDescription.ENVIRONMENT_NOTES,
            nickname = "registerExternalDatalake")
    DetailedEnvironmentV4Response registerExternalDatalake(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String environmentName,
            @Valid RegisterDatalakeV4Request request);

    @POST
    @Path("/{name}/register_datalake_prerequisites")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = EnvironmentDescription.DATALAKE_PREREQUISITES, produces = MediaType.APPLICATION_JSON,
            nickname = "registerDatalakePrerequisites")
    DatalakePrerequisiteV4Response registerDatalakePrerequisite(
            @PathParam("workspaceId") Long workspaceId,
            @PathParam("name") String environmentName,
            @Valid DatalakePrerequisiteV4Request datalakePrerequisiteV4Request);
}
