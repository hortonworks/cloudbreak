package com.sequenceiq.cloudbreak.api.endpoint.v4.environment;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.requests.EnvironmentAttachV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.requests.EnvironmentChangeCredentialRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.requests.EnvironmentDetachRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.requests.EnvironmentRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.requests.RegisterDatalakeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.responses.DetailedEnvironmentV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.responses.SimpleEnvironmentResponses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.responses.SimpleEnvironmentV4Response;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.EnvironmentOpDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v4/{workspaceId}/environments")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v4/{workspaceId}/environments", description = ControllerDescription.ENVIRONMENT_V3_DESCRIPTION, protocols = "http,https")
public interface EnvironmentV4Endpoint {

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = EnvironmentOpDescription.CREATE, produces = ContentType.JSON, notes = Notes.ENVIRONMENT_NOTES,
            nickname = "createEnvironment")
    DetailedEnvironmentV4Response create(@PathParam("workspaceId") Long workspaceId, @Valid EnvironmentRequest request);

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

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = EnvironmentOpDescription.LIST, produces = ContentType.JSON, notes = Notes.ENVIRONMENT_NOTES,
            nickname = "listEnvironment")
    SimpleEnvironmentResponses list(@PathParam("workspaceId") Long workspaceId);

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
                                                  @Valid EnvironmentDetachRequest request);

    @PUT
    @Path("/{name}/core/src/test/java/com/sequenceiq/cloudbreak/service/environment/EnvironmentServiceTest.java")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = EnvironmentOpDescription.CHANGE_CREDENTIAL, produces = ContentType.JSON, notes = Notes.ENVIRONMENT_NOTES,
            nickname = "changeCredentialInEnvironment")
    DetailedEnvironmentV4Response changeCredential(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String environmentName,
        @Valid EnvironmentChangeCredentialRequest request);

    @PUT
    @Path("/{name}/registerDatalake")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = EnvironmentOpDescription.REGISTER_EXTERNAL_DATALAKE, produces = ContentType.JSON, notes = Notes.ENVIRONMENT_NOTES,
            nickname = "registerExternalDatalake")
    DetailedEnvironmentV4Response registerExternalDatalake(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String environmentName,
        @Valid RegisterDatalakeV4Request request);
}
